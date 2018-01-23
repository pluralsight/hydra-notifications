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

import java.io.File
import java.lang.reflect.Modifier

import akka.actor.Props
import com.github.vonnagy.service.container.ContainerBuilder
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import com.typesafe.config.ConfigFactory
import configs.syntax._
import hydra.notifications.services.NotificationsSupervisor
import org.apache.commons.lang3.ClassUtils
import org.reflections.Reflections

object NotificationsService extends App {

  val config = ConfigFactory.load()
    .withFallback(ConfigFactory.parseFile(new File("/etc/hydra/hydra-notifications.conf")))

  val endpoints = config.get[List[String]]("hydra.notifications.endpoints").valueOrElse(Seq.empty)
    .map(Class.forName(_).asInstanceOf[Class[_ <: RoutedEndpoints]])

  val builder = ContainerBuilder()
    .withConfig(config)
    .withRoutes(endpoints: _*)
    .withName("hydra")
    .withActors("notifications_supervisor" -> Props(classOf[NotificationsSupervisor], notificationServices))

  val container = builder.build

  container.start()

  private def notificationServices: Map[String, Props] = {
    import scala.collection.JavaConverters._
    val reflections = new Reflections("hydra.notifications.services")

    reflections.getSubTypesOf(classOf[HydraNotificationService])
      .asScala.filterNot(c => Modifier.isAbstract(c.getModifiers))
      .map(c => ClassUtils.getShortCanonicalName(c).toLowerCase -> Props(c)).toMap

  }

}
