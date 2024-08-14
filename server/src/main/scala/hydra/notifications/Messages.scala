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
package hydra.notifications

import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest.PriorityEnum
import hydra.notifications.client.HydraNotification
import spray.json.{DefaultJsonProtocol, DeserializationException, JsObject, JsString, JsValue, RootJsonFormat, enrichAny}

trait HydraNotificationMessage

/**
 * Just a tag interface for reflection loading
 */
trait HydraNotificationService

case class Notify(notification: HydraNotification) extends HydraNotificationMessage

case class NotificationSent(result: String) extends HydraNotificationMessage

case class NotificationSendError(code: Int, error: String) extends HydraNotificationMessage

case class StreamsNotification(level: String,
                               message: String,
                               properties: Map[String, String],
                               stackTrace: String,
                               timestamp: String)

sealed trait Payload

case class MessageString(value: String) extends Payload

case class MessageJson(value: StreamsNotification) extends Payload

object PayloadJsonProtocol extends DefaultJsonProtocol {
  implicit object PayloadFormat extends RootJsonFormat[Payload] {
    def write(payload: Payload): JsValue = payload match {
      case MessageString(value) => JsString(value)
      case MessageJson(value)   => value.toJson
    }

    def read(value: JsValue): Payload = value match {
      case JsString(str) => MessageString(str)
      case obj: JsObject => MessageJson(obj.convertTo[StreamsNotification])
      case _             => throw DeserializationException("Payload must be either a string or a JSON object.")
    }
  }

  implicit val streamsNotificationFormat: RootJsonFormat[StreamsNotification] = jsonFormat5(StreamsNotification)
}

object ImplicitConversions {
  implicit def stringToPriority(value: String): PriorityEnum = {
    val priorityString = value.toUpperCase
    if (PriorityEnum.values.map(_.toString).contains(priorityString)) {
      PriorityEnum.fromValue(priorityString)
    } else {
      throw new IllegalArgumentException(s"Invalid priority value. Valid values are: ${PriorityEnum.values().mkString(", ")}.")
    }
  }
}
