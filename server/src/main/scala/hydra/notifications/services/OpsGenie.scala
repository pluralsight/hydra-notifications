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
import com.ifountain.opsgenie.client.OpsGenieClient
import com.ifountain.opsgenie.client.swagger.ApiException
import com.ifountain.opsgenie.client.swagger.model.{CreateAlertRequest, Recipient, TeamRecipient}
import com.typesafe.config.ConfigFactory
import hydra.notifications._
import hydra.notifications.client.OpsGenieNotification

import scala.collection.JavaConverters._
import scala.util.Try

class OpsGenie extends Actor with ActorLogging with HydraNotificationService {

  private val config = ConfigFactory.load()

  private val token = config.getString("hydra.notifications.services.opsgenie.token")

  private val client = new OpsGenieClient().alertV2()

  override def preStart(): Unit = client.getApiClient().setApiKey(token)

  override def receive: Receive = {
    case Notify(opsGenie: OpsGenieNotification) =>
      val response = Try(client.createAlert(alertRequest(opsGenie)))
        .map(r => NotificationSent(r.getResult))
        .recover { case e: ApiException => NotificationSendError(e.getCode, e.getMessage) }
        .get

      sender ! response
  }

  private[services] def alertRequest(n: OpsGenieNotification): CreateAlertRequest = {
    val team = new TeamRecipient().name(n.team)
    val request = new CreateAlertRequest()
    request.setMessage(n.message)
    request.setAlias(n.alias)
    n.description.foreach(request.setDescription)
    request.setTeams(Seq(team).asJava)
    request.setVisibleTo(Seq(team.asInstanceOf[Recipient]).asJava)
    request.setTags(n.tags.asJava)
    request.setEntity(n.entity)
    n.source.foreach(request.setSource)
    request.setPriority(CreateAlertRequest.PriorityEnum.P2)
    request.setUser(n.user)
    n.note.foreach(request.setNote)
    request
  }
}


