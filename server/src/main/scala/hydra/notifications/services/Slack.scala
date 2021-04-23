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
package hydra.notifications.services

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import com.typesafe.config.ConfigFactory
import hydra.notifications._
import hydra.notifications.client.SlackNotification
import slack.api.SlackApiClient
import slack.models.Channel

import scala.concurrent.Future

class Slack extends Actor with ActorLogging with HydraNotificationService {

  private val config = ConfigFactory.load()

  private val token = config.getString("hydra.notifications.services.slack.token")

  private val slackClient = SlackApiClient(token)

  private implicit val ec = context.dispatcher

  private implicit val system = context.system

  override def receive = {
    case Notify(slack: SlackNotification) =>
      val requestor = sender
      val response = slackClient.postChatMessage(slack.channel, slack.message).map(NotificationSent(_))
        .recover { case e: Exception => NotificationSendError(400, e.getMessage) }

      pipe(response) to requestor
  }
}