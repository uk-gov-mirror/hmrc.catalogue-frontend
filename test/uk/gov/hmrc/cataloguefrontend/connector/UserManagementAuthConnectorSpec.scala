/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.cataloguefrontend.connector

import java.util.UUID

import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.cataloguefrontend.connector.UserManagementAuthConnector._
import uk.gov.hmrc.cataloguefrontend.connector.model.Username
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier}

import scala.concurrent.ExecutionContext
import scala.util.Random

class UserManagementAuthConnectorSpec
  extends AnyWordSpec
  with Matchers
  with HttpClientStub
  with MockitoSugar
  with ScalaFutures {
  import ExecutionContext.Implicits.global

  "authenticate" should {

    "return authorization token when UMP auth service returns OK containing it" in new Setup {
      val token  = UUID.randomUUID().toString
      val userId = "john.smith"

      expect
        .POST(
          to      = s"$userMgtAuthUrl/v1/login",
          payload = Json.obj("username" -> username, "password" -> password)
        )
        .returning(
          status = OK,
          json   = Json.obj("Token" -> token, "uid" -> userId)
        )

      connector.authenticate(username, password).futureValue shouldBe Right(
        TokenAndUserId(UmpToken(token), UmpUserId(userId)))
    }

    UNAUTHORIZED :: FORBIDDEN :: Nil foreach { status =>
      s"return unauthorized when UMP auth service returns $status" in new Setup {
        expect
          .POST(
            to      = s"$userMgtAuthUrl/v1/login",
            payload = Json.obj("username" -> username, "password" -> password)
          )
          .returning(status)

        connector.authenticate(username, password).futureValue shouldBe Left(UmpUnauthorized)
      }
    }

    CREATED +: NOT_FOUND +: INTERNAL_SERVER_ERROR +: Nil foreach { status =>
      s"throw BadGatewayException for unrecognized $status status" in new Setup {
        expect
          .POST(
            to      = s"$userMgtAuthUrl/v1/login",
            payload = Json.obj("username" -> username, "password" -> password)
          )
          .returning(status)

        val e = connector.authenticate(username, password).failed.futureValue
        e shouldBe an[BadGatewayException]
        e.getMessage shouldBe s"Received $status from POST to $userMgtAuthUrl/v1/login"
      }
    }
  }

  "isValid" should {
    "return User if UMP Auth service returns 200 OK" in new Setup {
      val umpToken = UmpToken("value")
      expect
        .GET(to = s"$userMgtAuthUrl/v1/login")(headerCarrier.withExtraHeaders("Token" -> umpToken.value))
        .returning(
          status = OK,
          json   = Json.obj("uid" -> username, "groups" -> JsArray(Seq.empty))
        )

      connector.getUser(umpToken).futureValue shouldBe Some(User(Username("username"), groups = List.empty))
    }

    UNAUTHORIZED :: FORBIDDEN :: Nil foreach { status =>
      s"return false if UMP returns $status status" in new Setup {
        val umpToken = UmpToken("value")
        expect
          .GET(to = s"$userMgtAuthUrl/v1/login")(headerCarrier.withExtraHeaders("Token" -> umpToken.value))
          .returning(status)

        connector.getUser(umpToken).futureValue shouldBe None
      }
    }

    "throw BadGatewayException if UMP Auth Service returns other unexpected status" in new Setup {
      val umpToken = UmpToken("value")
      val unsupportedStatusCodes =
        Random
          .shuffle((201 to 599).filterNot(s => s == UNAUTHORIZED || s == FORBIDDEN))
          .take(10)

      unsupportedStatusCodes.foreach { unsupportedStatus =>
        expect
          .GET(to = s"$userMgtAuthUrl/v1/login")(headerCarrier.withExtraHeaders("Token" -> umpToken.value))
          .returning(unsupportedStatus)

        val e = connector.getUser(umpToken).failed.futureValue
        e shouldBe an[BadGatewayException]
        e.getMessage shouldBe s"Received $unsupportedStatus from GET to $userMgtAuthUrl/v1/login"
      }
    }
  }

  private trait Setup {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val username                              = "username"
    val password                              = "password"
    val userManagementAuthConfig              = mock[UserManagementAuthConfig]
    val userMgtAuthUrl                        = "http://usermgt-auth:9999"

    when(userManagementAuthConfig.baseUrl)
      .thenReturn(userMgtAuthUrl)

    val connector = new UserManagementAuthConnector(httpClient, userManagementAuthConfig)
  }
}
