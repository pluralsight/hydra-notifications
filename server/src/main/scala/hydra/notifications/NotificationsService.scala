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

import java.lang.reflect.Modifier

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import hydra.notifications.http.{HealthEndpoint, NotificationsEndpoint}
import hydra.notifications.services.NotificationsSupervisor
import org.apache.commons.lang3.ClassUtils
import org.reflections.Reflections

object NotificationsService extends App with RouteConcatenation {


  implicit val system = ActorSystem()

  implicit val materializer = ActorMaterializer()

  private val config = ConfigFactory.load

  private val httpPort = config.getInt("container.http.port")

  val notificationsSupervisor = system.actorOf(Props(classOf[NotificationsSupervisor],
    notificationServices), "notifications_supervisor")

  val routes = HealthEndpoint.routes ~ new NotificationsEndpoint(notificationsSupervisor).routes

  val server = Http().bindAndHandle(routes, "0.0.0.0", httpPort)

  private def notificationServices: Map[String, Props] = {
    import scala.collection.JavaConverters._
    val reflections = new Reflections("hydra.notifications.services")

    reflections.getSubTypesOf(classOf[HydraNotificationService])
      .asScala.filterNot(c => Modifier.isAbstract(c.getModifiers))
      .map(c => ClassUtils.getShortCanonicalName(c).toLowerCase -> Props(c)).toMap
  }
}
