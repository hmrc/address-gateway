/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.addressgateway.controllers

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.addressgateway.ToggledAuthorisedFunctions
import uk.gov.hmrc.addressgateway.config.AppConfig
import uk.gov.hmrc.addressgateway.connector.DownstreamConnector
import uk.gov.hmrc.auth.core.AuthProvider.StandardApplication
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class AddressInsightsController @Inject() (cc: ControllerComponents, config: AppConfig, connector: DownstreamConnector, val authConnector: AuthConnector)(
  implicit ec: ExecutionContext
) extends BackendController(cc)
    with ToggledAuthorisedFunctions {

  private val logger = Logger(this.getClass.getSimpleName)

  def lookup(): Action[AnyContent] = Action.async { implicit request =>
    toggledAuthorised(config.rejectInternalTraffic, AuthProviders(StandardApplication)) {
      val path = downstreamUri(request.target.uri.toString, "address-lookup")
      val url = s"${config.lookupBaseUrl}$path"

      connector.forward(request, url, config.internalAuthToken)
    }
  }
  def insights(): Action[AnyContent] = Action.async { implicit request =>
    toggledAuthorised(config.rejectInternalTraffic, AuthProviders(StandardApplication)) {
      val path = downstreamUri(request.target.uri.toString, "address-reputation")
      val url = s"${config.reputationBaseUrl}$path"

      connector.forward(request, url, config.internalAuthToken)
    }
  }

  private def downstreamUri(uri: String, targetServiceContext: String): String =
    uri.toString.replace(config.appName, targetServiceContext)

  def checkConnectivity(): Unit = {
    val insightsUrl = s"${config.reputationBaseUrl}/address-reputation/reputation/sa-reg"
    val lookupUrl = s"${config.lookupBaseUrl}/address-lookup/lookup"
    val checkInsights = connector.checkConnectivity(
      insightsUrl,
      config.internalAuthToken
    )
    val checkLookup = connector.checkConnectivity(
      lookupUrl,
      config.internalAuthToken
    )

    checkInsights.flatMap(i => checkLookup.map(l => (i, l))).map {
      case (true, true) =>
        logger.info("Connectivity to address-reputation and address-lookup established")
      case (true, false) =>
        logger.error("ERROR: Could not connect to address-lookup")
      case (false, true) =>
        logger.error("ERROR: Could not connect to address-reputation")
      case (false, false) =>
        logger.error("ERROR: Could not connect to address-reputation or address-lookup")
    }
  }

  checkConnectivity()
}
