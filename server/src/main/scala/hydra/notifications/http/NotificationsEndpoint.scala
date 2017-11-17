/*
 * Copyright (C) 2017 Pluralsight, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package hydra.notifications.http

import akka.actor.{ActorRef, ActorRefFactory, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import hydra.notifications._
import hydra.notifications.client.{HydraNotification, NotificationsResponse}
import hydra.notifications.services.NotificationsSupervisor.{GetServiceList, SendNotification, ServiceList, ServiceNotFound}
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._

class NotificationsEndpoint(implicit val system: ActorSystem, implicit val actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints with Directives with SprayJsonSupport with DefaultJsonProtocol {

  import hydra.notifications.client.NotificationsFormat._

  implicit val timeout = Timeout(5.seconds)

  private val notificationsActor = system.actorSelection("/user/service/notifications_supervisor").resolveOne()

  override val route = onSuccess(notificationsActor) { actor =>
    path("/notify") {
      post {
        entity(as[HydraNotification]) {
          notify(actor, _)
        }
      } ~ getServices(actor)
    }
  }


  private def getServices(supervisor: ActorRef): Route = get {
    onSuccess(supervisor ? GetServiceList) { msg =>
      msg match {
        case ServiceList(svcs) => complete(OK, svcs)
        case r => complete(400, NotificationsResponse(400, r.toString))
      }
    }
  }

  private def notify(supervisor: ActorRef, notification: HydraNotification): Route = {
    onSuccess(supervisor ? SendNotification(notification)) { msg =>
      msg match {
        case NotificationSent(message) => complete(OK, NotificationsResponse(200, message))
        case ServiceNotFound(s) => complete(NotFound, NotificationsResponse(404, s"Service $s not found."))
        case NotificationSendError(code, error) => complete(code, NotificationsResponse(code, error))
      }
    }
  }
}

