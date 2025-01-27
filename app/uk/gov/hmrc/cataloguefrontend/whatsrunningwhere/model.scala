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

import org.apache.http.client.utils.URIBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.cataloguefrontend.connector.model.{TeamName, Version}
import uk.gov.hmrc.cataloguefrontend.model.Environment

import java.net.URI
import java.time.Instant

sealed trait Platform {
  def asString: String
  def displayName: String
}

case class WhatsRunningWhere(
  applicationName: ServiceName,
  versions: List[WhatsRunningWhereVersion]
)

case class WhatsRunningWhereVersion(
  environment: Environment,
  versionNumber: VersionNumber,
  lastSeen: TimeSeen
)

object JsonCodecs {
  def format[A, B](f: A => B, g: B => A)(implicit fa: Format[A]): Format[B] = fa.inmap(f, g)

  private def toResult[A](e: Either[String, A]): JsResult[A] =
    e match {
      case Right(r) => JsSuccess(r)
      case Left(l)  => JsError(__, l)
    }

  val applicationNameFormat: Format[ServiceName] = format(ServiceName.apply, unlift(ServiceName.unapply))
  val versionNumberFormat: Format[VersionNumber] = format(VersionNumber.apply, unlift(VersionNumber.unapply))
  val timeSeenFormat: Format[TimeSeen]           = format(TimeSeen.apply, unlift(TimeSeen.unapply))
  val teamNameFormat: Format[TeamName]           = format(TeamName.apply, unlift(TeamName.unapply))
  val usernameReads: Format[Username]            = format(Username.apply, unlift(Username.unapply))
  val deploymentStatusFormat: Format[DeploymentStatus] =
    format(DeploymentStatus.apply, unlift(DeploymentStatus.unapply))

  val environmentFormat: Format[Environment] = new Format[Environment] {
    override def reads(json: JsValue): JsResult[Environment] =
      json
        .validate[String]
        .flatMap { s =>
          Environment.parse(s) match {
            case Some(env) => JsSuccess(env)
            case None      => JsError(__, s"Invalid Environment '$s'")
          }
        }

    override def writes(e: Environment) =
      JsString(e.asString)
  }

  val whatsRunningWhereVersionReads: Reads[WhatsRunningWhereVersion] = {
    implicit val wf  = environmentFormat
    implicit val vnf = versionNumberFormat
    implicit val tsf = timeSeenFormat
    ((__ \ "environment").read[Environment]
      ~ (__ \ "versionNumber").read[VersionNumber]
      ~ (__ \ "lastSeen").read[TimeSeen])(WhatsRunningWhereVersion.apply _)
  }

  val whatsRunningWhereReads: Reads[WhatsRunningWhere] = {
    implicit val wf    = applicationNameFormat
    implicit val wrwvf = whatsRunningWhereVersionReads
    ((__ \ "applicationName").read[ServiceName]
      ~ (__ \ "versions").read[List[WhatsRunningWhereVersion]])(WhatsRunningWhere.apply _)
  }

  val profileTypeFormat: Format[ProfileType] = new Format[ProfileType] {
    override def reads(js: JsValue): JsResult[ProfileType] =
      js.validate[String]
        .flatMap(s => toResult(ProfileType.parse(s)))

    override def writes(et: ProfileType): JsValue =
      JsString(et.asString)
  }

  val profileNameFormat: Format[ProfileName] =
    format(ProfileName.apply, unlift(ProfileName.unapply))

  val profileFormat: OFormat[Profile] = {
    implicit val ptf = profileTypeFormat
    implicit val pnf = profileNameFormat
    ((__ \ "type").format[ProfileType]
      ~ (__ \ "name").format[ProfileName])(Profile.apply, unlift(Profile.unapply))
  }

  // Deployment Event
  val deploymentEventFormat: Format[DeploymentEvent] = {
    implicit val vnf = versionNumberFormat
    implicit val tsf = timeSeenFormat
    implicit val dsf = deploymentStatusFormat
    ((__ \ "deploymentId").format[String]
      ~ (__ \ "status").format[DeploymentStatus]
      ~ (__ \ "version").format[VersionNumber]
      ~ (__ \ "time").format[TimeSeen])(DeploymentEvent.apply, unlift(DeploymentEvent.unapply))
  }

  val serviceDeploymentsFormat: Format[ServiceDeployment] = {
    implicit val devf = deploymentEventFormat
    implicit val anf  = applicationNameFormat
    implicit val enf  = environmentFormat
    ((__ \ "serviceName").format[ServiceName]
      ~ (__ \ "environment").format[Environment]
      ~ (__ \ "deploymentEvents").format[Seq[DeploymentEvent]]
      ~ (__ \ "lastCompleted").formatNullable[DeploymentEvent])(ServiceDeployment.apply, unlift(ServiceDeployment.unapply))
  }

  val deploymentHistoryReads: Reads[DeploymentHistory] = {
    implicit val anf = applicationNameFormat
    implicit val ef  = environmentFormat
    implicit val vnf = versionNumberFormat
    implicit val tsf = timeSeenFormat
    implicit val dar = usernameReads
    implicit val tmf = teamNameFormat
    ((__ \ "serviceName").read[ServiceName]
      ~ (__ \ "environment").read[Environment]
      ~ (__ \ "version").read[VersionNumber]
      ~ (__ \ "teams").read[Seq[TeamName]]
      ~ (__ \ "time").read[TimeSeen]
      ~ (__ \ "username").read[Username])(DeploymentHistory.apply _)
  }
}

case class TimeSeen(time: Instant)

object TimeSeen {
  implicit val timeSeenOrdering: Ordering[TimeSeen] = {
    implicit val io: Ordering[Instant] = Ordering.by(_.toEpochMilli)
    Ordering.by(_.time)
  }
}

case class ServiceName(asString: String) extends AnyVal
object ServiceName {
  implicit val serviceNameOrdering: Ordering[ServiceName] =
    Ordering.by(_.asString)
}

case class ProfileName(asString: String) extends AnyVal

object ProfileName {
  implicit val ordering: Ordering[ProfileName] =
    Ordering.by(_.asString)
}

sealed trait ProfileType {
  def asString: String
}
object ProfileType {
  def parse(s: String): Either[String, ProfileType] =
    values
      .find(_.asString == s)
      .toRight(s"Invalid profileType - should be one of: ${values.map(_.asString).mkString(", ")}")

  val values: List[ProfileType] = List(
    Team,
    ServiceManager
  )
  case object Team extends ProfileType {
    override val asString: String = "team"
  }
  case object ServiceManager extends ProfileType {
    override val asString: String = "servicemanager"
  }
}

case class Profile(
  profileType: ProfileType,
  profileName: ProfileName
)

case class VersionNumber(asString: String) extends AnyVal {
  def asVersion: Version = Version(asString)
}

case class DeploymentStatus(asString: String) extends AnyVal

case class DeploymentEvent(
  deploymentId: String,
  status: DeploymentStatus,
  version: VersionNumber,
  time: TimeSeen
)

case class ServiceDeployment(
  serviceName: ServiceName,
  environment: Environment,
  deploymentEvents: Seq[DeploymentEvent],
  lastCompleted: Option[DeploymentEvent]
)

case class PaginatedDeploymentHistory(history: Seq[DeploymentHistory], total: Long)

case class DeploymentHistory(
  name: ServiceName,
  environment: Environment,
  version: VersionNumber,
  teams: Seq[TeamName],
  time: TimeSeen,
  username: Username
)

case class Username(asString: String)

case class Pagination(page: Int, pageSize: Int, total: Long)

object Pagination {
  def uriForPage(uri: String, page: Int): URI =
    new URIBuilder(uri)
      .setParameter("page", page.toString)
      .build()
}
