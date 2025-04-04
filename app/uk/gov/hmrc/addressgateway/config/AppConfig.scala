/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.addressgateway.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")
  val rejectInternalTraffic: Boolean = config.getOptional[Boolean]("microservice.rejectInternalTraffic").getOrElse(false)
  val internalAuthToken: String = config.get[String]("microservice.services.internal-auth.token")
  val addressReputationBaseUrl: String = servicesConfig.baseUrl("address-reputation")
  val lookupBaseUrl: String = servicesConfig.baseUrl("address-lookup")
}
