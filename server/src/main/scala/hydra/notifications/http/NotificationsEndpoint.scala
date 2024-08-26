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

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers._
import akka.pattern.ask
import akka.util.Timeout
import hydra.notifications._
import hydra.notifications.client.{HydraNotification, NotificationsResponse, OpsGenieNotification, SlackNotification}
import hydra.notifications.services.NotificationsSupervisor.{GetServiceList, SendNotification, ServiceList, ServiceNotFound}
import spray.json.DefaultJsonProtocol
import hydra.notifications.PayloadJsonProtocol._
import hydra.notifications.converters.{Converter, CsvToHtmlConverter}

import scala.concurrent.duration._
import spray.json._

import scala.util.Try

class NotificationsEndpoint(notificationsSupervisor: ActorRef)
  extends Directives with SprayJsonSupport with DefaultJsonProtocol {

  implicit val timeout: Timeout = Timeout(5.seconds)

  def combinedRoute(supervisor: ActorRef): Route =
    post {
      path("notify" / "opsgenie") {
        entity(as[String]) { message =>
          parameters('priority, 'alias, 'note.?, 'team, "tags".as(CsvSeq[String]), 'entity,
            'source.?, 'user) { (priority, alias, noteOpt, team, tags, entity, sourceOpt, user) =>
            try {
              val payload = extractPayload(message)
              val (title, description, details) = extractData(payload)
              val opsGenieNotification = OpsGenieNotification(title, priority, alias, description, noteOpt, team, tags, entity, sourceOpt, user, details)

              notify(supervisor, opsGenieNotification)
            } catch {
              case ex: DeserializationException => complete(StatusCodes.BadRequest, ex.getMessage)
            }
          }
        }
      } ~ path("notify" / "slack") {
        entity(as[String]) { message =>
          parameters('channel) { channel =>
            val notification = SlackNotification(channel, message)
            notify(supervisor, notification)
          }
        }
      }
    }

  val routes: Route = path("notify") {
    getServices(notificationsSupervisor)
  } ~ combinedRoute(notificationsSupervisor)


  import hydra.notifications.client.NotificationsFormat._

  private def getServices(supervisor: ActorRef): Route = get {
    onSuccess(supervisor ? GetServiceList) {
      case ServiceList(svcs) => complete(OK, svcs)
      case r => complete(400, NotificationsResponse(400, r.toString))
    }
  }

  private def notify(supervisor: ActorRef, notification: HydraNotification): Route = {
    onSuccess(supervisor ? SendNotification(notification)) {
      case NotificationSent(message) => complete(OK, NotificationsResponse(200, message))
      case ServiceNotFound(s) => complete(NotFound, NotificationsResponse(404, s"Service $s not found."))
      case NotificationSendError(code, error) => complete(code, NotificationsResponse(code, error))
    }
  }

  private def extractPayload(message: String): Payload = Try(message.parseJson) match {
    case util.Success(json) => json.convertTo[Payload] // Convert to Payload if valid JSON
    case util.Failure(_) => MessageString(message) // Handle as plain string if not valid JSON
  }

  private def extractData(payload: Payload): (String, Option[String], Option[Map[String, String]]) = {
    implicit class RichSet[A](set: Set[A]) {
      def containsNot(elem: A): Boolean = !set.contains(elem)
    }

    payload match {
      case MessageString(value) => (value, None, None)
      case MessageJson(notification) =>
        val (csvProperties, properties) = notification.properties.partition { case (_, value) =>
          CsvToHtmlConverter.isCsv(Converter.unescape(value))
        }

        val htmlProperties = csvProperties.mapValues(v => CsvToHtmlConverter.convertToHtml(Converter.unescape(v)))
        val filterKeys = htmlProperties.keySet ++ Set("description")

        (
          notification.message,
          notification.properties.get("description").map(Converter.unescape),
          Option(properties.filterKeys(filterKeys.containsNot) ++ htmlProperties)
        )
    }
  }
}
