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

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results._
import play.api.routing.sird.{POST => SPOST, _}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.core.server.{Server, ServerConfig}

class AddressInsightsControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  val insightsPort = 11222

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.address-reputation.port" -> insightsPort)
    .build()

  private val controller = app.injector.instanceOf[AddressInsightsController]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  "POST /lookup" should {

    "forward a 200 response from the downstream lookup service" in {
      val response = """[{
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
                       |}]""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/lookup") =>
          Action(Ok(response).withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))
        }
      } { _ =>
        val requestAddressJson = Json
          .parse("""{
                   | "postcode":"BB00 0BB"
                   |}""".stripMargin)
          .as[JsObject]
        val fakeRequest = FakeRequest("POST", "/lookup")
          .withJsonBody(requestAddressJson)
          .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

        val result = controller.insights()(fakeRequest)
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe response
      }
    }
  }

  "POST /insights" should {

    "forward a 200 response from the downstream service" in {
      val response = """{
                       |"correlationId": "some-correlation-id",
                       |"address":{
                       | "line1":"1 High Street",
                       | "country":"United Kingdom"
                       |},
                       |"insights":{
                       |   "risk":{
                       |      "riskScore": 100,
                       |      "reason": "ADDRESS_ON_WATCH_LIST"
                       |   },
                       |   "relationships":{
                       |      "occurrences":[]
                       |   }
                       |},
                       |}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/insights") =>
          Action(Ok(response).withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))
        }
      } { _ =>
        val requestAddressJson = Json
          .parse("""{
            | "address":{
            |   "line1": "1 High Street",
            |   "country": "United Kingdom"
            | }
            |}""".stripMargin)
          .as[JsObject]
        val fakeRequest = FakeRequest("POST", "/insights")
          .withJsonBody(requestAddressJson)
          .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

        val result = controller.insights()(fakeRequest)
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe response
      }
    }

    "forward a 400 response from the downstream service" in {
      val errorResponse = """{"code": "MALFORMED_JSON", "path.missing: address"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/insights") =>
          Action(
            BadRequest(errorResponse).withHeaders(
              HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
            )
          )
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/insights")
          .withJsonBody(Json.parse("""{"no-address": {}}"""))
          .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

        val result = controller.insights()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe errorResponse
      }
    }

    "handle a malformed json payload" in {
      val errorResponse = """{"code": "MALFORMED_JSON", "path.missing: address"}""".stripMargin

      Server.withRouterFromComponents(ServerConfig(port = Some(insightsPort))) { components =>
        import components.{defaultActionBuilder => Action}
        { case r @ SPOST(p"/insights") =>
          Action(BadRequest(errorResponse).withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON))
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/insights")
          .withTextBody("""{""")
          .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

        val result = controller.insights()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsString(result) shouldBe errorResponse
      }
    }

    "return bad gateway if there is no connectivity to the downstream service" in {
      val errorResponse = """{"code": "REQUEST_DOWNSTREAM", "desc": "An issue occurred when the downstream service tried to handle the request"}""".stripMargin

      val fakeRequest = FakeRequest("POST", "/address-gateway/insights")
        .withJsonBody(Json.parse("""{"address": "AB123456C"}"""))
        .withHeaders("True-Calling-Client" -> "example-service", HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)

      val result = controller.insights()(fakeRequest)
      status(result) shouldBe Status.BAD_GATEWAY
      contentAsString(result) shouldBe errorResponse
    }

  }
}
