/*
 * Copyright 2018 HM Revenue & Customs
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

import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.cataloguefrontend.connector.UserManagementAuthConnector.{TokenAndUserId, UmpToken, UmpUnauthorized, UmpUserId}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier}

class UserManagementAuthConnectorSpec extends WordSpec with HttpClientStub with MockitoSugar with ScalaFutures {

  "authenticate" should {

    "return authorization token when POST to usermgt/v1/login returns OK containing it" in new Setup {
      val token  = UUID.randomUUID().toString
      val userId = "john.smith"

      expect
        .POST(
          to      = s"$userMgtAuthUrl/v1/login",
          payload = Json.obj("username" -> username, "password" -> password)
        )
        .returning(
          status = OK,
          body   = Json.obj("Token" -> token, "uid" -> userId)
        )

      connector.authenticate(username, password).futureValue shouldBe Right(
        TokenAndUserId(UmpToken(token), UmpUserId(userId)))
    }

    "return unauthorized when POST to usermgt/v1/login returns UNAUTHORIZED" in new Setup {
      expect
        .POST(
          to      = s"$userMgtAuthUrl/v1/login",
          payload = Json.obj("username" -> username, "password" -> password)
        )
        .returning(UNAUTHORIZED)

      connector.authenticate(username, password).futureValue shouldBe Left(UmpUnauthorized)
    }

    CREATED +: NOT_FOUND +: INTERNAL_SERVER_ERROR +: Nil foreach { status =>
      s"throw BadGatewayException for unrecognized $status status" in new Setup {
        expect
          .POST(
            to      = s"$userMgtAuthUrl/v1/login",
            payload = Json.obj("username" -> username, "password" -> password)
          )
          .returning(status)

        intercept[BadGatewayException] {
          await(connector.authenticate(username, password))
        }.message shouldBe s"Received $status from POST to http://usermgt-auth:9999/v1/login"
      }
    }
  }

  private trait Setup {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val username                              = "username"
    val password                              = "password"
    val userManagementAuthConfig              = mock[UserManagementAuthConfig]
    val userMgtAuthUrl                        = "http://usermgt-auth:9999"

    when(userManagementAuthConfig.baseUrl()).thenReturn(userMgtAuthUrl)

    val connector = new UserManagementAuthConnector(httpClient, userManagementAuthConfig)
  }
}
