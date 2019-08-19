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

package uk.gov.hmrc.cataloguefrontend.shuttering

import java.time.Instant
import play.api.libs.json.{Format, Json, JsError, JsObject, JsValue, JsPath, JsResult, JsString, JsSuccess, Reads, Writes, __}
import play.api.libs.functional.syntax._


sealed trait Environment { def asString: String }
object Environment {
  case object Production      extends Environment { val asString = "production"   }
  case object ExternalTest    extends Environment { val asString = "externalTest" }
  case object QA              extends Environment { val asString = "qa"           }
  case object Staging         extends Environment { val asString = "staging"      }
  case object Dev             extends Environment { val asString = "development"  }

  val values = List(Production, ExternalTest, QA, Staging, Dev)

  def parse(s: String): Option[Environment] =
    values.find(_.asString == s)

  val format: Format[Environment] = new Format[Environment] {
    override def reads(json: JsValue) =
      json.validate[String]
        .flatMap { s =>
            parse(s) match {
              case Some(env) => JsSuccess(env)
              case None      => JsError(__, s"Invalid Environment '$s'")
            }
          }

    override def writes(e: Environment) =
      JsString(e.asString)
  }
}

sealed trait ShutterStatusValue { def asString: String }
object ShutterStatusValue {
  case object Shuttered   extends ShutterStatusValue { val asString = "shuttered"   }
  case object Unshuttered extends ShutterStatusValue { val asString = "unshuttered" }

  val values = List(Shuttered, Unshuttered)

  def parse(s: String): Option[ShutterStatusValue] =
    values.find(_.asString == s)


  val format: Format[ShutterStatusValue] = new Format[ShutterStatusValue] {
    override def reads(json: JsValue) =
      json.validate[String]
        .flatMap { s =>
            parse(s) match {
              case Some(env) => JsSuccess(env)
              case None      => JsError(__, s"Invalid ShutterStatusValue '$s'")
            }
          }

    override def writes(e: ShutterStatusValue) =
      JsString(e.asString)
  }
}

sealed trait ShutterStatus { def value: ShutterStatusValue }
object ShutterStatus {
  case class Shuttered(
      reason       : Option[String]
    , outageMessage: Option[String]
    ) extends ShutterStatus  { def value = ShutterStatusValue.Shuttered }
  case object Unshuttered extends ShutterStatus { def value = ShutterStatusValue.Unshuttered }

  val format: Format[ShutterStatus] =
    new Format[ShutterStatus] {
      override def reads(json: JsValue) = {
        implicit val ssvf = ShutterStatusValue.format
        (json \ "status").validate[ShutterStatusValue]
          .flatMap {
            case ShutterStatusValue.Unshuttered => JsSuccess(Unshuttered)
            case ShutterStatusValue.Shuttered   => JsSuccess(Shuttered(
                                                       reason        = (json \ "reason"       ).asOpt[String]
                                                     , outageMessage = (json \ "outageMessage").asOpt[String]
                                                     ))
            case s             => JsError(__, s"Invalid ShutterStatus '$s'")
          }
      }

      override def writes(ss: ShutterStatus): JsValue = {
        implicit val ssvf = ShutterStatusValue.format
        ss match {
          case Shuttered(reason, outageMessage) =>
            Json.obj(
                "status"        -> Json.toJson(ss.value)
              , "reason"        -> reason
              , "outageMessage" -> outageMessage
              )
          case Unshuttered =>
            Json.obj(
                "status" -> Json.toJson(ss.value)
              )
        }
      }
    }
}


case class ShutterState(
    name        : String
  , production  : ShutterStatus
  , staging     : ShutterStatus
  , qa          : ShutterStatus
  , externalTest: ShutterStatus
  , development : ShutterStatus
  ) {
    def statusFor(env: Environment): ShutterStatus =
      env match {
        case Environment.Production   => production
        case Environment.ExternalTest => externalTest
        case Environment.QA           => qa
        case Environment.Staging      => staging
        case Environment.Dev          => development
      }

    def isShuttered(env: String): Boolean =
      Environment.parse(env).exists(e => statusFor(e).value.asString == ShutterStatusValue.Shuttered.asString)
  }

object ShutterState {

  val reads: Reads[ShutterState] = {
    implicit val ssf = ShutterStatus.format
    ( (__ \ "name"        ).read[String]
    ~ (__ \ "production"  ).read[ShutterStatus]
    ~ (__ \ "staging"     ).read[ShutterStatus]
    ~ (__ \ "qa"          ).read[ShutterStatus]
    ~ (__ \ "externalTest").read[ShutterStatus]
    ~ (__ \ "development" ).read[ShutterStatus]
    )(ShutterState.apply _)
  }
}



// -------------- Events ---------------------


sealed trait EventType { def asString: String }
object EventType {
  case object ShutterStateCreate    extends EventType { override val asString = "shutter-state-create"    }
  case object ShutterStateDelete    extends EventType { override val asString = "shutter-state-delete"    }
  case object ShutterStateChange    extends EventType { override val asString = "shutter-state-change"    }
  case object KillSwitchStateChange extends EventType { override val asString = "killswitch-state-change" }


  val values = List(ShutterStateCreate, ShutterStateDelete, ShutterStateChange, KillSwitchStateChange)

  def parse(s: String): Option[EventType] =
    values.find(_.asString == s)

  val format: Format[EventType] = new Format[EventType] {
    override def reads(json: JsValue) =
      json.validate[String]
        .flatMap { s =>
            parse(s) match {
              case Some(et) => JsSuccess(et)
              case None     => JsError(__, s"Invalid EventType '$s'")
            }
          }

    override def writes(e: EventType) =
      JsString(e.asString)
  }
}

sealed trait ShutterCause { def asString: String }
object ShutterCause {
  case object Scheduled   extends ShutterCause { override val asString = "scheduled"   }
  case object UserCreated extends ShutterCause { override val asString = "user-shutter"}

  val values = List(Scheduled, UserCreated)

  def parse(s: String): Option[ShutterCause] =
    values.find(_.asString == s)

  val format: Format[ShutterCause] = new Format[ShutterCause] {
    override def reads(json: JsValue) =
      json.validate[String]
        .flatMap { s =>
            parse(s) match {
              case Some(et) => JsSuccess(et)
              case None     => JsError(__, s"Invalid ShutterCause '$s'")
            }
          }

    override def writes(e: ShutterCause) =
      JsString(e.asString)
  }

}

sealed trait EventData
object EventData {
  case class ShutterStateCreateData(
      serviceName: String
    ) extends EventData

  case class ShutterStateDeleteData(
      serviceName: String
    ) extends EventData

  case class ShutterStateChangeData(
      serviceName: String
    , environment: Environment
    , status     : ShutterStatus
    , cause      : ShutterCause
    ) extends EventData

  case class KillSwitchStateChangeData(
      environment: Environment
    , status     : ShutterStatusValue
    ) extends EventData


  val shutterStateCreateDataFormat: Format[ShutterStateCreateData] =
    (__ \ "serviceName").format[String]
      .inmap( ShutterStateCreateData.apply
            , unlift(ShutterStateCreateData.unapply)
            )

  val shutterStateDeleteDataFormat: Format[ShutterStateDeleteData] =
    (__ \ "serviceName").format[String]
      .inmap( ShutterStateDeleteData.apply
            , unlift(ShutterStateDeleteData.unapply)
            )

  val shutterStateChangeDataFormat: Format[ShutterStateChangeData] = {
    implicit val ef   = Environment.format
    implicit val ssvf = ShutterStatus.format
    implicit val scf  = ShutterCause.format

    ( (__ \ "serviceName").format[String]
    ~ (__ \ "environment").format[Environment]
    ~ (__ \ "status"     ).format[ShutterStatus]
    ~ (__ \ "cause"      ).format[ShutterCause]
    )( ShutterStateChangeData.apply
     , unlift(ShutterStateChangeData.unapply)
     )
  }

  val killSwitchStateChangeDataFormat: Format[KillSwitchStateChangeData] = {
    implicit val ef   = Environment.format
    implicit val ssvf = ShutterStatusValue.format

    ( (__ \ "environment").format[Environment]
    ~ (__ \ "status"     ).format[ShutterStatusValue]
    )( KillSwitchStateChangeData.apply
     , unlift(KillSwitchStateChangeData.unapply
     ))
  }

  def reads(et: EventType) = new Reads[EventData] {
    implicit val sscrdf = shutterStateCreateDataFormat
    implicit val ssddf  = shutterStateDeleteDataFormat
    implicit val sscdf  = shutterStateChangeDataFormat
    implicit val kscdf  = killSwitchStateChangeDataFormat
    def reads(js: JsValue): JsResult[EventData] =
      et match {
        case EventType.ShutterStateCreate    => js.validate[EventData.ShutterStateCreateData]
        case EventType.ShutterStateDelete    => js.validate[EventData.ShutterStateDeleteData]
        case EventType.ShutterStateChange    => js.validate[EventData.ShutterStateChangeData]
        case EventType.KillSwitchStateChange => js.validate[EventData.KillSwitchStateChangeData]
      }
  }
}

case class ShutterEvent(
    username : String
  , timestamp: Instant
  , eventType: EventType
  , data     : EventData
  ) {
    def toShutterStateChangeEvent: Option[ShutterStateChangeEvent] =
      data match {
        case sscd: EventData.ShutterStateChangeData =>
                    Some(ShutterStateChangeEvent(
                            username    = username
                          , timestamp   = timestamp
                          , serviceName = sscd.serviceName
                          , environment = sscd.environment
                          , status      = sscd.status
                          , cause       = sscd.cause
                          ))
        case _ => None
      }
  }

/** Special case flattened */
case class ShutterStateChangeEvent(
    username   : String
  , timestamp  : Instant
  , serviceName: String
  , environment: Environment
  , status     : ShutterStatus
  , cause      : ShutterCause
  )

object ShutterEvent {

  val reads: Reads[ShutterEvent] = {
    implicit val etf = EventType.format
    ( (__ \ "username" ).read[String]
    ~ (__ \ "timestamp").read[Instant]
    ~ (__ \ "type"     ).read[EventType]
    ~ (__ \ "type"     ).read[EventType]
                        .flatMap[EventData](et => (__ \ "data").read(EventData.reads(et)))
    )(ShutterEvent.apply _)
  }
}

case class TemplatedContent(
    elementId: String
  , innerHtml: String
  )

object TemplatedContent {
  val format: Format[TemplatedContent] =
    ( (__ \ "elementID").format[String]
    ~ (__ \ "innerHTML").format[String]
    )( TemplatedContent.apply
     , unlift(TemplatedContent.unapply)
     )
}

case class OutagePageWarning(
    name   : String
  , message: String
  )

object OutagePageWarning {
  val reads: Reads[OutagePageWarning] =
    ( (__ \ "type"   ).read[String]
    ~ (__ \ "message").read[String]
    )(OutagePageWarning.apply _)
}

case class OutagePage(
    serviceName      : String
  , environment      : Environment
  , outagePageURL    : String
  , warnings         : List[OutagePageWarning]
  , templatedElements: List[TemplatedContent]
  ) {
    def templatedMessages: List[TemplatedContent] =
      templatedElements
        .filter(_.elementId == "templatedMessage")
  }

object OutagePage {
  val reads: Reads[OutagePage] = {
    implicit val ef   = Environment.format
    implicit val tcf  = TemplatedContent.format
    implicit val opwr = OutagePageWarning.reads
    ( (__ \ "serviceName"      ).read[String]
    ~ (__ \ "environment"      ).read[Environment]
    ~ (__ \ "outagePageURL"    ).read[String]
    ~ (__ \ "warnings"         ).read[List[OutagePageWarning]]
    ~ (__ \ "templatedElements").read[List[TemplatedContent]]
    )(OutagePage.apply _)
  }
}

case class OutagePageStatus(
    serviceName : String
  , warning     : Option[(String, String)]
  )
