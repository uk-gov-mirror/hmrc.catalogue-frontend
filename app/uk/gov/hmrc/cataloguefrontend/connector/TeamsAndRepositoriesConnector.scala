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

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.cataloguefrontend.connector.DigitalService.DigitalServiceRepository
import uk.gov.hmrc.cataloguefrontend.connector.model.TeamName
import uk.gov.hmrc.cataloguefrontend.model.Environment
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object RepoType extends Enumeration {

  type RepoType = Value

  val Service, Library, Prototype, Other = Value

  val format: Format[RepoType] =
    new Format[RepoType] {
      override def reads(json: JsValue) =
        json match {
          case JsString(s) => JsSuccess(RepoType.withName(s))
          case _ => JsError(
                      __,
                     JsonValidationError(s"Expected value to be a String contained within ${RepoType.values}, got $json instead."))
        }

      override def writes(rt: RepoType) =
        JsString(rt.toString)
    }
}

case class Link(name: String, displayName: String, url: String) {
  val id: String = displayName.toLowerCase.replaceAll(" ", "-")
}

object Link {
  val format: Format[Link] =
    Json.format[Link]
}

case class JenkinsLink(service: String, jenkinsURL: String)

case class TargetEnvironment(environment: Environment, services: Seq[Link]) {
  val id: String = environment.asString
}

object TargetEnvironment {
  val format: Format[TargetEnvironment] = {
    implicit val ef = TeamsAndRepositoriesEnvironment.format
    implicit val lf = Link.format
    ( (__ \ "name"    ).format[Environment]
    ~ (__ \ "services").format[Seq[Link]]
    )(TargetEnvironment.apply, unlift(TargetEnvironment.unapply))
  }
}

case class RepositoryDetails(
  name        : String,
  description : String,
  createdAt   : LocalDateTime,
  lastActive  : LocalDateTime,
  owningTeams : Seq[TeamName],
  teamNames   : Seq[TeamName],
  githubUrl   : Link,
  jenkinsURL  : Option[Link],
  environments: Option[Seq[TargetEnvironment]],
  repoType    : RepoType.RepoType,
  isPrivate   : Boolean,
  isArchived  : Boolean
)

object RepositoryDetails {
  val format: Format[RepositoryDetails] = {
    implicit val tnf = TeamName.format
    implicit val lf  = Link.format
    implicit val tef = TargetEnvironment.format
    implicit val rtf = RepoType.format
    Json.format[RepositoryDetails]
  }
}

case class RepositoryDisplayDetails(
  name         : String,
  createdAt    : LocalDateTime,
  lastUpdatedAt: LocalDateTime,
  repoType     : RepoType.RepoType)

object RepositoryDisplayDetails {
  val format: OFormat[RepositoryDisplayDetails] = {
    implicit val rtf = RepoType.format
    Json.format[RepositoryDisplayDetails]
  }
}

case class Team(
  name                    : TeamName,
  firstActiveDate         : Option[LocalDateTime],
  lastActiveDate          : Option[LocalDateTime],
  firstServiceCreationDate: Option[LocalDateTime],
  repos                   : Option[Map[String, Seq[String]]]
) {
  //Teams and repos lists legacy java services as 'Other', so relaxing the auth check to be agnostic to repo type
  def allServiceNames: List[String] = repos.getOrElse(Map.empty).values.flatten.toList
}

object Team {
  val format: OFormat[Team] = {
    implicit val tnf = TeamName.format
    Json.format[Team]
  }
}

case class DigitalService(name: String, lastUpdatedAt: Long, repositories: Seq[DigitalServiceRepository])

object DigitalService {
  case class DigitalServiceRepository(
    name         : String,
    createdAt    : LocalDateTime,
    lastUpdatedAt: LocalDateTime,
    repoType     : RepoType.RepoType,
    teamNames    : Seq[TeamName])

  object DigitalServiceRepository {
    val format: OFormat[DigitalServiceRepository] = {
      implicit val rtf = RepoType.format
      implicit val tnf = TeamName.format
      Json.format[DigitalServiceRepository]
    }
  }

  val format: OFormat[DigitalService] = {
    implicit val dsrf = DigitalServiceRepository.format
    Json.format[DigitalService]
  }
}

object TeamsAndRepositoriesEnvironment {
  val format: Format[Environment] =
    // The source of environment is url-templates.envrionments (sic)
    // https://github.com/hmrc/app-config-base/blob/227e006901c96ad10230a2f88a305b70645bf7d1/teams-and-repositories.conf
    new Format[Environment] {
      override def reads(json: JsValue) =
        json.validate[String]
          .flatMap {
            case "Production"    => JsSuccess(Environment.Production)
            case "External Test" => JsSuccess(Environment.ExternalTest)
            case "QA"            => JsSuccess(Environment.QA)
            case "Staging"       => JsSuccess(Environment.Staging)
            case "Integration"   => JsSuccess(Environment.Integration)
            case "Development"   => JsSuccess(Environment.Development)
            case s               => JsError(__, s"Invalid Environment '$s'")
          }

      override def writes(e: Environment) =
        JsString(e.asString)
    }
}

@Singleton
class TeamsAndRepositoriesConnector @Inject()(
  http          : HttpClient,
  servicesConfig: ServicesConfig,
  configuration:  Configuration
)(implicit val ec: ExecutionContext
) {
  import TeamsAndRepositoriesConnector._
  import HttpReads.Implicits._

  private val logger = Logger(getClass)

  private val teamsAndServicesBaseUrl: String =
    servicesConfig.baseUrl("teams-and-repositories")

  private implicit val tf   = Team.format
  private implicit val tnf  = TeamName.format
  private implicit val jf   = Json.format[JenkinsLink]
  private implicit val rdf  = RepositoryDetails.format
  private implicit val rddf = RepositoryDisplayDetails.format
  private implicit val dsf  = DigitalService.format

  def lookupLink(service: String)(implicit hc: HeaderCarrier): Future[Option[Link]] = {
    val url = url"$teamsAndServicesBaseUrl/api/jenkins-url/$service"
    http.GET[JenkinsLink](url)
      .map(l => Link(l.service, "Build", l.jenkinsURL))
      .map(Option.apply)
      .recover {
        case NonFatal(ex) =>
          logger.error(s"An error occurred when connecting to $url: ${ex.getMessage}", ex)
          None
      }
    }

  def allTeams(implicit hc: HeaderCarrier): Future[Seq[Team]] =
    http.GET[Seq[Team]](url"$teamsAndServicesBaseUrl/api/teams")

  def allDigitalServices(implicit hc: HeaderCarrier): Future[Seq[String]] =
    http.GET[Seq[String]](url"$teamsAndServicesBaseUrl/api/digital-services")

  def teamInfo(teamName: TeamName)(implicit hc: HeaderCarrier): Future[Option[Team]] = {
    val url = url"$teamsAndServicesBaseUrl/api/teams_with_details/${teamName.asString}"
    http
      .GET[Option[Team]](url)
      .recover {
        case NonFatal(ex) =>
          logger.error(s"An error occurred when connecting to $url: ${ex.getMessage}", ex)
          None
      }
    }

  def teamsWithRepositories(implicit hc: HeaderCarrier): Future[Seq[Team]] =
    http.GET[Seq[Team]](url"$teamsAndServicesBaseUrl/api/teams_with_repositories")

  def digitalServiceInfo(digitalServiceName: String)(implicit hc: HeaderCarrier): Future[Option[DigitalService]] =
    http.GET[Option[DigitalService]](url"$teamsAndServicesBaseUrl/api/digital-services/$digitalServiceName")

  def allRepositories(implicit hc: HeaderCarrier): Future[Seq[RepositoryDisplayDetails]] =
    repositories(archived = None)

  def archivedRepositories(implicit hc: HeaderCarrier): Future[Seq[RepositoryDisplayDetails]] =
    repositories(archived = Some(true))

  def repositoryDetails(name: String)(implicit hc: HeaderCarrier): Future[Option[RepositoryDetails]] =
    http.GET[Option[RepositoryDetails]](url"$teamsAndServicesBaseUrl/api/repositories/$name")

  def teamsByService(serviceNames: Seq[String])(implicit hc: HeaderCarrier): Future[Map[ServiceName, Seq[TeamName]]] =
    http.POST[JsValue, Map[ServiceName, Seq[TeamName]]](
      url"$teamsAndServicesBaseUrl/api/services?teamDetails=true",
      Json.arr(serviceNames.map(toJsFieldJsValueWrapper(_)): _*)
    )

  def allTeamsByService()(implicit hc: HeaderCarrier): Future[Map[ServiceName, Seq[TeamName]]] =
    http.GET[Map[ServiceName, Seq[TeamName]]](
      url"$teamsAndServicesBaseUrl/api/services?teamDetails=true"
    )

  private def repositories(archived: Option[Boolean])(implicit hc: HeaderCarrier): Future[Seq[RepositoryDisplayDetails]] =
    http.GET[Seq[RepositoryDisplayDetails]](
      url"$teamsAndServicesBaseUrl/api/repositories?archived=${archived.map(_.toString)}"
    )
}

object TeamsAndRepositoriesConnector {

  type ServiceName = String

  sealed trait TeamsAndRepositoriesError

  case class HTTPError(code: Int) extends TeamsAndRepositoriesError

  case class ConnectionError(exception: Throwable) extends TeamsAndRepositoriesError {
    override def toString: String = exception.getMessage
  }
}
