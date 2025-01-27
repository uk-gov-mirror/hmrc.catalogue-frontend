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

package uk.gov.hmrc.cataloguefrontend.whatsrunningwhere

import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.scalatest._
import play.api.libs.ws._
import uk.gov.hmrc.cataloguefrontend.util.UnitSpec
import uk.gov.hmrc.cataloguefrontend.{FakeApplicationBuilder, JsonData, WireMockEndpoints}

class WhatsRunningWhereSpec extends UnitSpec with BeforeAndAfter with FakeApplicationBuilder with WireMockEndpoints {

  private[this] lazy val WS = app.injector.instanceOf[WSClient]

  "What's running where page" should {
    "show a list of applications, environments and version numbers for releases" in {

      serviceEndpoint(GET, "/api/teams_with_repositories", willRespondWith = (200, Some(JsonData.teamsWithRepos)))

      serviceEndpoint(GET, "/releases-api/profiles", willRespondWith = (200, Some(JsonData.profiles)))

      serviceEndpoint(
        GET,
        "/releases-api/whats-running-where",
        queryParameters = Seq.empty,
        willRespondWith = (
          200,
          Some("""[
                 |  {
                 |    "applicationName": "api-definition",
                 |    "versions": [
                 |      {
                 |        "environment": "integration",
                 |        "versionNumber": "1.58.0",
                 |        "lastSeen": "2019-05-29T14:09:48Z"
                 |      }
                 |    ]
                 |  },
                 |  {
                 |    "applicationName": "api-documentation",
                 |    "versions": [
                 |      {
                 |        "environment": "integration",
                 |        "versionNumber": "0.41.0",
                 |        "lastSeen": "2019-05-29T14:09:46Z"
                 |      }
                 |    ]
                 |  }
                 |]""".stripMargin))
      )

      val response = WS.url(s"http://localhost:$port/whats-running-where").get.futureValue

      response.status shouldBe 200

      response.body should include("api-definition")
      response.body should include("1.58.0")

      response.body should include("api-documentation")
      response.body should include("0.41.0")

      response.body should include("Integration")
    }
  }
}
