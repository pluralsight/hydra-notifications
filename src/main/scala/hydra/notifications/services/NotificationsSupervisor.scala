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

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import hydra.notifications.{HydraNotification, HydraNotificationMessage, Notify}
import hydra.notifications.services.NotificationsSupervisor._

class NotificationsSupervisor(services: Map[String, Props]) extends Actor with ActorLogging {

  private var serviceActors: Map[String, ActorRef] = _

  override def preStart(): Unit = {
    serviceActors = services.map(s => s._1 -> context.actorOf(s._2))
  }

  override def receive = {
    case SendNotification(notification) =>
      serviceActors.get(notification.service) match {
        case Some(actor) => actor forward Notify(notification)
        case None => sender ! ServiceNotFound(notification.service)
      }

    case GetServiceList => sender ! ServiceList(serviceActors.keySet.toSeq)
  }
}

object NotificationsSupervisor {

  case class SendNotification(notification: HydraNotification) extends HydraNotificationMessage

  case class ServiceNotFound(serviceName: String) extends HydraNotificationMessage

  case object GetServiceList extends HydraNotificationMessage

  case class ServiceList(services: Seq[String]) extends HydraNotificationMessage

}
