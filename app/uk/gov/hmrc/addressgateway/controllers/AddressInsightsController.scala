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
      val url = s"${config.addressReputationBaseUrl}$path"

      connector.forward(request, url, config.internalAuthToken)
    }
  }

  private def downstreamUri(uri: String, targetServiceContext: String): String =
    uri.replace(config.appName, targetServiceContext)

}
