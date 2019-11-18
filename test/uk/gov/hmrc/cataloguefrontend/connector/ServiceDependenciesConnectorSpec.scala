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

package uk.gov.hmrc.cataloguefrontend.connector

import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.apache.http.HttpStatus
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.cataloguefrontend.WireMockEndpoints
import uk.gov.hmrc.cataloguefrontend.connector.model.{Dependency, JDK, JRE, OpenJDK, Oracle, Version}
import uk.gov.hmrc.cataloguefrontend.service.{DependenciesService, ServiceDependencies}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class ServiceDependenciesConnectorSpec
    extends FreeSpec
    with Matchers
    with BeforeAndAfter
    with GuiceOneAppPerSuite
    with WireMockEndpoints
    with EitherValues
    with OptionValues
    with ScalaFutures
    with MockitoSugar
    with IntegrationPatience {
  import ExecutionContext.Implicits.global

  override def fakeApplication: Application =
    new GuiceApplicationBuilder()
      .disable(classOf[com.kenshoo.play.metrics.PlayModule])
      .configure(
        Map(
          "microservice.services.service-dependencies.port" -> endpointPort,
          "microservice.services.service-dependencies.host" -> host,
          "metrics.jvm"                                     -> false
        ))
      .build()

  private lazy val serviceDependenciesConnector = app.injector.instanceOf[ServiceDependenciesConnector]

  "GET Dependencies" - {

    "return a list of dependencies for a repository" in new Setup {

      serviceEndpoint(
        GET,
        "/api/dependencies/repo1",
        willRespondWith = (
          200,
          Some(
            """{
          |  "repositoryName": "repo1",
          |  "libraryDependencies": [
          |    {
          |      "name": "frontend-bootstrap",
          |      "currentVersion": "7.11.0",
          |      "latestVersion": "8.80.0",
          |      "isExternal": false,
          |      "bobbyRuleViolations": []
          |    },
          |    {
          |      "name": "play-config",
          |      "currentVersion": "3.0.0",
          |      "latestVersion": "7.70.0",
          |      "isExternal": false,
          |      "bobbyRuleViolations": []
          |    }
          |  ],
          |  "sbtPluginsDependencies": [
          |    {
          |      "name": "plugin-1",
          |      "currentVersion": "1.0.0",
          |      "latestVersion": "1.1.0",
          |      "isExternal": true,
          |      "bobbyRuleViolations": []
          |    },
          |    {
          |      "name": "plugin-2",
          |      "currentVersion": "2.0.0",
          |      "latestVersion": "2.1.0",
          |      "isExternal": false,
          |      "bobbyRuleViolations": []
          |    }
          |  ],
          |  "otherDependencies": [
          |    {
          |      "name": "sbt",
          |      "currentVersion": "0.13.8",
          |      "latestVersion": "0.13.15",
          |      "isExternal": false,
          |      "bobbyRuleViolations": []
          |    }
          |  ],
          |  "lastUpdated": "2017-11-08T16:31:38.975Z"
          |}""".stripMargin
          ))
      )

      val response = serviceDependenciesConnector
        .getDependencies("repo1")
        .futureValue
        .value

      response.libraryDependencies.size shouldBe 2

      response.repositoryName      shouldBe "repo1"
      response.libraryDependencies should contain theSameElementsAs
        Seq(
          Dependency("frontend-bootstrap", Version("7.11.0"), Some(Version("8.80.0"))),
          Dependency("play-config", Version("3.0.0"), Some(Version("7.70.0")))
        )

      response.sbtPluginsDependencies should contain theSameElementsAs
        Seq(
          Dependency("plugin-1", Version("1.0.0"), Some(Version("1.1.0")), isExternal = true),
          Dependency("plugin-2", Version("2.0.0"), Some(Version("2.1.0")), isExternal = false)
        )
      response.otherDependencies should contain theSameElementsAs
        Seq(
          Dependency("sbt", Version("0.13.8"), Some(Version("0.13.15")))
        )

    }

    "return a None for non existing repository" in new Setup {

      serviceEndpoint(GET, "/api/dependencies/non-existing-repo", willRespondWith = (404, None))

      val response = serviceDependenciesConnector
        .getDependencies("non-existing-repo")
        .futureValue

      response shouldBe None

    }

    "return a None for if a communication error occurs" in new Setup {

      val mockedHttpClient = mock[HttpClient]
      when(mockedHttpClient.GET(any())(any(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Boom!!")))

      val connector = new ServiceDependenciesConnector(mockedHttpClient, mock[ServicesConfig])

      connector
        .getDependencies("non-existing-repo")
        .futureValue shouldBe None
    }
  }

  "GET all dependencies for report" - {

    "return dependencies for all repositories" in new Setup {

      serviceEndpoint(
        GET,
        "/api/dependencies",
        willRespondWith = (
          200,
          Some(
            """[
          |  {
          |    "repositoryName": "repo1",
          |    "libraryDependencies": [
          |      {
          |        "name": "frontend-bootstrap",
          |        "currentVersion": "7.11.0",
          |        "latestVersion": "8.80.0",
          |        "isExternal": false,
          |        "bobbyRuleViolations": []
          |      },
          |      {
          |        "name": "play-config",
          |        "currentVersion": "3.0.0",
          |        "latestVersion": "7.70.0",
          |        "isExternal": false,
          |        "bobbyRuleViolations": []
          |      }
          |    ],
          |    "sbtPluginsDependencies": [
          |      {
          |        "name": "plugin-1",
          |        "currentVersion": "1.0.0",
          |        "latestVersion": "1.1.0",
          |        "isExternal": true,
          |        "bobbyRuleViolations": []
          |      },
          |      {
          |        "name": "plugin-2",
          |        "currentVersion": "2.0.0",
          |        "latestVersion": "2.1.0",
          |        "isExternal": false,
          |        "bobbyRuleViolations": []
          |      }
          |    ],
          |    "otherDependencies": [
          |      {
          |        "name": "sbt",
          |        "currentVersion": "0.13.7",
          |        "latestVersion": "0.13.15",
          |        "isExternal": false,
          |        "bobbyRuleViolations": []
          |      }
          |    ],
          |    "lastUpdated": "2017-11-08T16:31:38.975Z"
          |  },
          |  {
          |    "repositoryName": "repo2",
          |    "libraryDependencies": [
          |      {
          |        "name": "some-lib-1",
          |        "currentVersion": "7.77.0",
          |        "latestVersion": "8.80.0",
          |        "isExternal": false,
          |        "bobbyRuleViolations": []
          |      },
          |      {
          |        "name": "some-lib-2",
          |        "currentVersion": "3.0.0",
          |        "latestVersion": "7.70.0",
          |        "isExternal": false,
          |        "bobbyRuleViolations": []
          |      }
          |    ],
          |    "sbtPluginsDependencies": [
          |      {
          |        "name": "plugin-3",
          |        "currentVersion": "1.0.0",
          |        "latestVersion": "1.1.0",
          |        "isExternal": true,
          |        "bobbyRuleViolations": []
          |      },
          |      {
          |        "name": "plugin-4",
          |        "currentVersion": "2.0.0",
          |        "latestVersion": "2.1.0",
          |        "isExternal": false,
          |        "bobbyRuleViolations": []
          |      }
          |    ],
          |    "otherDependencies": [
          |      {
          |        "name": "sbt",
          |        "currentVersion": "0.13.8",
          |        "latestVersion": "0.13.15",
          |        "isExternal": false,
          |        "bobbyRuleViolations": []
          |      }
          |    ],
          |    "lastUpdated": "2017-11-08T16:31:38.975Z"
          |  }
          |]""".stripMargin
          ))
      )

      val response = serviceDependenciesConnector
        .getAllDependencies()
        .futureValue

      response.size shouldBe 2

      response.head.libraryDependencies.size shouldBe 2

      response.head.repositoryName      shouldBe "repo1"
      response.head.libraryDependencies should contain theSameElementsAs
        Seq(
          Dependency("frontend-bootstrap", Version("7.11.0"), Some(Version("8.80.0"))),
          Dependency("play-config", Version("3.0.0"), Some(Version("7.70.0")))
        )

      response.head.sbtPluginsDependencies should contain theSameElementsAs
        Seq(
          Dependency("plugin-1", Version("1.0.0"), Some(Version("1.1.0")), isExternal = true),
          Dependency("plugin-2", Version("2.0.0"), Some(Version("2.1.0")), isExternal = false)
        )

      response.head.otherDependencies should contain theSameElementsAs
        Seq(
          Dependency("sbt", Version("0.13.7"), Some(Version("0.13.15")))
        )

      response.last.libraryDependencies.size shouldBe 2

      response.last.repositoryName      shouldBe "repo2"
      response.last.libraryDependencies should contain theSameElementsAs
        Seq(
          Dependency("some-lib-1", Version("7.77.0"), Some(Version("8.80.0"))),
          Dependency("some-lib-2", Version("3.0.0"), Some(Version("7.70.0")))
        )

      response.last.sbtPluginsDependencies should contain theSameElementsAs
        Seq(
          Dependency("plugin-3", Version("1.0.0"), Some(Version("1.1.0")), isExternal = true),
          Dependency("plugin-4", Version("2.0.0"), Some(Version("2.1.0")), isExternal = false)
        )

      response.last.otherDependencies should contain theSameElementsAs
        Seq(
          Dependency("sbt", Version("0.13.8"), Some(Version("0.13.15")))
        )
    }
  }

  "GET curated slug dependencies" - {
    "return a list of curated dependencies for a known slug" in new Setup {
      val SlugName = "slug-name"
      val SlugVersion = "slug-version"
      serviceEndpoint(GET, url = s"/api/slug-dependencies/$SlugName/$SlugVersion",
        willRespondWith = (Status.OK, Some(
          """|[{
             |  "name": "dep1",
             |  "currentVersion": {"major": 1, "minor": 0, "patch": 0, "original": "1.0.0"},
             |  "bobbyRuleViolations": [],
             |  "isExternal": false
             | },
             | {"name": "dep2",
             |  "currentVersion": {"major": 2, "minor": 0, "patch": 0, "original": "2.0.0"},
             |  "latestVersion": {"major": 2, "minor": 1, "patch": 0, "original": "2.1.0"},
             |  "bobbyRuleViolations": [],
             |  "isExternal": false
             | }]""".stripMargin)))

      val response = serviceDependenciesConnector.getCuratedSlugDependencies(SlugName, SlugVersion).futureValue

      response should contain theSameElementsAs Seq(
        Dependency(name = "dep1", currentVersion = Version("1.0.0"), latestVersion = None),
        Dependency(name = "dep2", currentVersion = Version("2.0.0"), latestVersion = Some(Version("2.1.0")))
      )
    }

    "return an empty list of dependencies for an unknown slug" in new Setup {
      val SlugName = "slug-name"
      val SlugVersion = "slug-version"
      serviceEndpoint(GET, url = s"/api/slug-dependencies/$SlugName/$SlugVersion",
        willRespondWith = (Status.NOT_FOUND, None))

      serviceDependenciesConnector.getCuratedSlugDependencies(SlugName, SlugVersion).futureValue shouldBe empty
    }

    "return an empty list of dependencies when a communication error occurs" in new Setup {
      val mockedHttpClient = mock[HttpClient]
      when(mockedHttpClient.GET(any())(any(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Boom!!")))

      val connector = new ServiceDependenciesConnector(mockedHttpClient, mock[ServicesConfig])

      connector.getCuratedSlugDependencies("slugName", "slugVersion").futureValue shouldBe empty
    }
  }

  "getJDKVersions" - {
    "returns JDK versions with vendor" in new Setup {
      serviceEndpoint(
        GET,
        s"/api/jdkVersions?flag=${SlugInfoFlag.Production.asString}",
        willRespondWith = (
          200,
          Some(
            """[
              | {"name":"something-api",  "version":"1.8.0_181", "vendor": "Oracle", "kind": "JDK"}
              |,{"name":"service-backend","version":"1.8.0_191", "vendor": "OpenJDK", "kind": "JRE"}
              |]""".stripMargin)))

      val response = serviceDependenciesConnector.getJDKVersions(SlugInfoFlag.Production).futureValue

      response.head.name    shouldBe "something-api"
      response.head.version shouldBe "1.8.0_181"
      response.head.vendor  shouldBe Oracle
      response.head.kind    shouldBe JDK

      response(1).name    shouldBe "service-backend"
      response(1).version shouldBe "1.8.0_191"
      response(1).vendor  shouldBe OpenJDK
      response(1).kind    shouldBe JRE

    }
  }

  "JSON Reader" - {
    "read json with java section" in {
      import uk.gov.hmrc.cataloguefrontend.service.ServiceDependencies.serviceDependenciesReads
      val json =""" {
    "uri" : "https://artefactory/slugs/mobile-stub/mobile-stub_0.12.0_0.5.2.tgz",
    "name" : "mobile-auth-stub",
    "version" : "0.12.0",
    "semanticVersion" : {
        "major" : 0,
        "minor" : 12,
        "patch" : 0
    },
    "versionLong" : 12000,
    "runnerVersion" : "0.5.2",
    "classpath" : "",
    "jdkVersion" : "1.8.0_191",
    "dependencies" : [
        {
            "path" : "./mobile-auth-stub-0.12.0/lib/org.slf4j.slf4j-api-1.7.25.jar",
            "version" : "1.7.25",
            "group" : "org.slf4j",
            "artifact" : "slf4j-api",
            "meta" : "fromPom"
        }
    ],
    "latest" : false,
    "qa" : false,
    "production" : false,
    "development" : false,
    "external test" : false,
    "staging" : false,
    "java" : {
        "version" : "1.8.0_191",
        "kind" : "JDK",
        "vendor" : "OpenJDK"
    }
} """
      val res = Json.fromJson[ServiceDependencies](Json.parse(json)).get

      res.java.version shouldBe "1.8.0_191"
      res.java.vendor  shouldBe "OpenJDK"
      res.java.kind    shouldBe "JDK"
    }

  }
  private trait Setup {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  }
}
