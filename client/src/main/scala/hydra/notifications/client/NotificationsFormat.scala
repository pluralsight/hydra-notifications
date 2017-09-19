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
package hydra.notifications.client

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue, RootJsonFormat, deserializationError, _}

object NotificationsFormat extends DefaultJsonProtocol with SprayJsonSupport {


  implicit val opsGenieFormat = jsonFormat(OpsGenieNotification,"message","alias","description","note","team","tags","entity","source","user")

  implicit val slackFormat = jsonFormat(SlackNotification,"channel","message")

  implicit val notificationsResponseFormat = jsonFormat2(NotificationsResponse)

  implicit val notificationsFormat = new RootJsonFormat[HydraNotification] {
    def write(obj: HydraNotification): JsValue =
      JsObject((obj match {
        case c: OpsGenieNotification => c.toJson
        case d: SlackNotification => d.toJson
      }).asJsObject.fields + ("service" -> JsString(obj.service)))

    def read(json: JsValue): HydraNotification =
      json.asJsObject.getFields("service") match {
        case Seq(JsString("opsgenie")) => json.convertTo[OpsGenieNotification]
        case Seq(JsString("slack")) => json.convertTo[SlackNotification]
        case _ => deserializationError("Known service types are opsgenie or slack.")
      }
  }
}
