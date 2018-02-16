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

package uk.gov.hmrc.cataloguefrontend.service

import org.scalatest.{Matchers, WordSpec}
import play.api.Configuration
import uk.gov.hmrc.cataloguefrontend.connector.RepositoryWithLeaks

class LeakDetectionServiceSpec extends WordSpec with Matchers {
  "Service" should {
    "determine if at least one of team's repos has leaks" in {
      val reposWithLeaks = List(RepositoryWithLeaks("repo1"), RepositoryWithLeaks("repo2"))
      val teamRepos1     = List("repo2")

      val service = new LeakDetectionService(null, configuration)

      service.leaksFoundForTeam(reposWithLeaks, teamRepos1) shouldBe true

      val teamRepos2 = List("repo3")
      service.leaksFoundForTeam(reposWithLeaks, teamRepos2) shouldBe false
    }
  }

  val configuration =
    Configuration(
      "microservice.services.leak-detection.productionUrl" -> "",
      "ldsIntegration.enabled"                             -> "false"
    )
}
