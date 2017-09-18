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

abstract class HydraNotification {
  def service: String
}

trait HydraNotificationMessage

/**
  * Just a tag interface for reflection loading
  */
trait HydraNotificationService

case class Notify(notification: HydraNotification) extends HydraNotificationMessage

case class NotificationSent(result: String) extends HydraNotificationMessage

case class NotificationSendError(code: Int, error: String) extends HydraNotificationMessage
