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

package uk.gov.hmrc.cataloguefrontend.healthindicators

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{HealthIndicatorsLeaderBoard, HealthIndicatorsPage, error_404_template}


import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HealthIndicatorsController @Inject()(
  healthIndicatorsConnector: HealthIndicatorsConnector,
  mcc: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends FrontendController(mcc) {

  def indicatorsForRepo(name: String): Action[AnyContent] =
    Action.async { implicit request =>
      healthIndicatorsConnector.getHealthIndicators(name).map {
        case Some(repositoryRating: RepositoryRating) => Ok(HealthIndicatorsPage(repositoryRating))
        case None => NotFound(error_404_template())
      }
    }

  def indicatorsForAllRepos(): Action[AnyContent] =
    Action.async { implicit request =>
      for {
        allIndicators <- healthIndicatorsConnector.getAllHealthIndicators()
        form = HealthIndicatorsFilter.form.bindFromRequest
        repoTypes = allIndicators.map(_.repositoryType).distinct
        filteredIndicators = form.fold(_ => None, _.repoType) match {
          case Some(rt) => allIndicators.filter(_.repositoryType == rt.asString)
          case None => allIndicators
        }
      } yield filteredIndicators match {
          case repoRatings: Seq[RepositoryRating] => Ok(HealthIndicatorsLeaderBoard(repoRatings, form, repoTypes))
          case _ => NotFound(error_404_template())
      }
    }
  }

object HealthIndicatorsController {
  def getScoreColour(score: Int): String = {
    score match {
      case x if x > 0 => "repo-score-green"
      case x if x > -100 => "repo-score-amber"
      case _ => "repo-score-red"
    }
  }
}

case class HealthIndicatorsFilter(
  repoType: Option[RepoType] = None
)

object HealthIndicatorsFilter {
  lazy val form: Form[HealthIndicatorsFilter] = Form(
    mapping(
      "repo_type" -> optional(text)
        .transform[Option[RepoType]](
          _.flatMap(s => RepoType.parse(s).toOption),
          _.map(_.asString)
        )
    )(HealthIndicatorsFilter.apply)(HealthIndicatorsFilter.unapply)
  )
}

sealed trait RepoType {
  def asString: String
}

object RepoType {
  def parse(s: String): Either[String, RepoType] = {
    values
      .find(_.asString == s)
      .toRight(s"Invalid repoType - should be one of: ${values.map(_.asString).mkString(", ")}")
  }

  val values: List[RepoType] = List(
    Service,
    Library,
    Prototype,
    Other
  )

  case object Service extends RepoType {
    override def asString: String = "Service"
  }
  case object Library extends RepoType{
    override def asString: String = "Library"
  }
  case object Prototype extends RepoType{
    override def asString: String = "Prototype"
  }
  case object Other extends RepoType{
    override def asString: String = "Other"
  }
}