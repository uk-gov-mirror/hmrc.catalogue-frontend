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

package uk.gov.hmrc.cataloguefrontend

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.cataloguefrontend.connector.model.Version
import uk.gov.hmrc.cataloguefrontend.service.DependenciesService
import uk.gov.hmrc.cataloguefrontend.whatsrunningwhere.WhatsRunningWhereService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.DependenciesPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DependenciesController @Inject()(
  mcc: MessagesControllerComponents,
  dependenciesService: DependenciesService,
  whatsRunningWhereService: WhatsRunningWhereService,
  dependenciesPage: DependenciesPage
)(implicit val ec: ExecutionContext)
    extends FrontendController(mcc) {

  def service(name: String): Action[AnyContent] = Action.async { implicit request =>
    for {
      deployments         <- whatsRunningWhereService.releasesForService(name).map(_.versions)
      serviceDependencies <- dependenciesService.search(name, deployments)
    } yield Ok(dependenciesPage(name, serviceDependencies.sortBy(_.semanticVersion)(Ordering[Option[Version]].reverse)))
  }
}
