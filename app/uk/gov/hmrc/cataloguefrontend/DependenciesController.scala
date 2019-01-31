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

package uk.gov.hmrc.cataloguefrontend

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.cataloguefrontend.service.{DependenciesService, DeploymentsService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.DependenciesPage
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DependenciesController @Inject()(mcc: MessagesControllerComponents,
                                       dependenciesService: DependenciesService,
                                       deploymentsService: DeploymentsService,
                                       dependenciesPage: DependenciesPage
) extends FrontendController(mcc) {

  def service(name: String): Action[AnyContent] = Action.async { implicit request =>
    for {
      deployments <- deploymentsService.getWhatsRunningWhere(name)
      serviceDependencies <- dependenciesService.search(name, deployments)
    } yield
      deployments match {
        case Left(t) => ServiceUnavailable(t.getMessage)
        case _ => Ok(dependenciesPage(name, serviceDependencies.sortBy(_.version)(Ordering[Option[String]].reverse)))
      }
  }
}