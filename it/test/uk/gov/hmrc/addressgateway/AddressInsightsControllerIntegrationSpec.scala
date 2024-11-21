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

package uk.gov.hmrc.addressgateway

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, equalToJson, post, urlEqualTo}
import org.apache.pekko.http.scaladsl.model.MediaTypes
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes}
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.test.ExternalWireMockSupport

class AddressInsightsControllerIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with ExternalWireMockSupport {

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:$port"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false,
        "microservice.services.address-insights-proxy.port" -> externalWireMockPort,
        "microservice.services.address-lookup.port" -> externalWireMockPort
      )
      .build()

  "AddressInsightsController" should {
    "respond with OK status" when {
      "/reputation/sa-reg called with valid json payload" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/address-insights/reputation/sa-reg"))
            .withRequestBody(equalToJson("""{"address":{"addressLine1":"1 High Street", "country":"United Kingdom"}}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
            .willReturn(
              aResponse()
                .withBody(
                  """{"correlationId":"220967234589763549876", "address":{"addressLine1":"1 High Street", "country":"United Kingdom"}, "lookbackDays": 10, "insights":  {"relationships": {"occurrences": {}}}}"""
                )
                .withStatus(OK)
            )
        )
        val response =
          wsClient
            .url(s"$baseUrl/address-gateway/reputation/sa-reg")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post("""{"address":{"addressLine1":"1 High Street", "country":"United Kingdom"}}""")
            .futureValue

        response.status shouldBe OK
        response.json shouldBe Json.parse(
          """{"correlationId":"220967234589763549876", "address":{"addressLine1":"1 High Street", "country":"United Kingdom"}, "lookbackDays": 10, "insights":  {"relationships": {"occurrences": {}}}}"""
        )
      }
    }

    "respond with OK status" when {
      "/lookup called with valid json payload" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/address-lookup/lookup"))
            .withRequestBody(equalToJson("""{"postcode":"BB00 0BB"}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
            .willReturn(
              aResponse()
                .withBody(
                  """{
                    |  "id": "GB200000706253",
                    |  "uprn": 200000706253,
                    |  "parentUprn": 200000706251,
                    |  "usrn": 300000706253,
                    |  "organisation": "Some Company",
                    |  "address": {
                    |    "lines": [
                    |      "Test House",
                    |      "The Tests"
                    |    ],
                    |    "town": "Test Town",
                    |    "postcode": "BB00 0BB",
                    |    "subdivision": {
                    |      "code": "GB-ENG",
                    |      "name": "England"
                    |    },
                    |    "country": {
                    |      "code": "GB",
                    |      "name": "United Kingdom"
                    |    }
                    |  },
                    |  "localCustodian": {
                    |    "code": 1760,
                    |    "name": "Test Valley"
                    |  },
                    |  "location": [
                    |    50.9986451,
                    |    -1.4690977
                    |  ],
                    |  "language": "en",
                    |  "administrativeArea": "TEST COUNTY                ]"
                    |}""".stripMargin
                )
                .withStatus(OK)
            )
        )
        val response =
          wsClient
            .url(s"$baseUrl/address-gateway/lookup")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post("""{"postcode":"BB00 0BB"}""")
            .futureValue

        response.status shouldBe OK
        response.json shouldBe Json.parse(
          """{
            |  "id": "GB200000706253",
            |  "uprn": 200000706253,
            |  "parentUprn": 200000706251,
            |  "usrn": 300000706253,
            |  "organisation": "Some Company",
            |  "address": {
            |    "lines": [
            |      "Test House",
            |      "The Tests"
            |    ],
            |    "town": "Test Town",
            |    "postcode": "BB00 0BB",
            |    "subdivision": {
            |      "code": "GB-ENG",
            |      "name": "England"
            |    },
            |    "country": {
            |      "code": "GB",
            |      "name": "United Kingdom"
            |    }
            |  },
            |  "localCustodian": {
            |    "code": 1760,
            |    "name": "Test Valley"
            |  },
            |  "location": [
            |    50.9986451,
            |    -1.4690977
            |  ],
            |  "language": "en",
            |  "administrativeArea": "TEST COUNTY                ]"
            |}""".stripMargin
        )
      }
    }

    "respond with BAD_REQUEST status" when {
      "invalid json payload is provided" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/address-insights/reputation/sa-reg"))
            .withRequestBody(equalToJson("""{"address":{"addressLine1":"1 High Street", "country":"United Kingdom"}}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MediaTypes.`application/json`.value))
            .willReturn(
              aResponse()
                .withBody(
                  """{"correlationId":"220967234589763549876", "address":{"addressLine1":"1 High Street", "country":"United Kingdom"}, "lookbackDays": 10, "insights":  {"relationships": {"occurrences": {}}}}"""
                )
                .withStatus(OK)
            )
        )
        val response =
          wsClient
            .url(s"$baseUrl/address-gateway/reputation/sa-reg")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post("""{"address":{"addressLine1":"1 High Street", "country":"United Kingdom"}""")
            .futureValue

        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.parse("""{"statusCode":400,"message":"bad request, cause: invalid json"}""")
      }
    }
  }
}
