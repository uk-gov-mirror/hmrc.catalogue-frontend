/*
 * Copyright 2019 HM Revenue & Customs
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

package view
import org.jsoup.Jsoup
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.cataloguefrontend.ViewMessages
import uk.gov.hmrc.cataloguefrontend.connector.SlugInfoFlag
import uk.gov.hmrc.cataloguefrontend.connector.SlugInfoFlag.Latest
import uk.gov.hmrc.cataloguefrontend.connector.model.JDKVersion
import views.html.JDKVersionPage



class JDKVersionPageSpec extends WordSpec with MockitoSugar with Matchers {
  private[this] val msg :ViewMessages = mock[ViewMessages]

  "JDK Usage list" should {

    "show a list of slugs and what JDK they're using" in {
      implicit val request = FakeRequest()

      val versions = List(JDKVersion("test-slug", "1.181.0"), JDKVersion("thing-service", "1.171.0"))
      val document = asDocument(new JDKVersionPage(msg)(versions, SlugInfoFlag.values, Latest))

      val slug1 = document.select("#jdk-slug-test-slug")
      val slug2 = document.select("#jdk-slug-thing-service")

      slug1.select("#jdk-slug-test-slug").text() shouldBe "test-slug 1.181.0"
      slug2.select("#jdk-slug-thing-service").text() shouldBe "thing-service 1.171.0"

    }


    "include a link to the repository" in {
      implicit val request = FakeRequest()

      val versions = List(JDKVersion("thing-service", "1.171.0"))
      val document = asDocument(new JDKVersionPage(msg)(versions, SlugInfoFlag.values, Latest))

      val slug = document.select("#jdk-slug-thing-service")
      val link = slug.select("a[href*='/repositories/thing-service']")

      link.attr("href") shouldBe "/repositories/thing-service"
    }
  }


  private def asDocument(html: Html) = Jsoup.parse(html.toString())
}
