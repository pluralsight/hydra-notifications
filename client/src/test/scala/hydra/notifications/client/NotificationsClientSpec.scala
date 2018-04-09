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

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.testkit.TestKit
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}

class NotificationsClientSpec extends TestKit(ActorSystem("test"))
  with Matchers with FunSpecLike with BeforeAndAfterAll with ScalaFutures with SprayJsonSupport {

  import NotificationsFormat._
  import spray.json._

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))

  val wireMockServer = new WireMockServer(options().dynamicPort())

  wireMockServer.stubFor(post(urlEqualTo("/notify"))
    .willReturn(aResponse()
      .withHeader("Content-Type", "application/json")
      .withBody(NotificationsResponse(200, "done").toJson.compactPrint)))

  lazy val client = new NotificationsClient("localhost", wireMockServer.port)

  override def beforeAll() = wireMockServer.start()

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
    client.shutdown()
    wireMockServer.stop()
  }

  describe("The Notifications Client") {
    it("sends a slack notification") {
      val response = client.postNotification(SlackNotification("test", "test"))
      whenReady(response) { r => r shouldBe NotificationsResponse(200, "done") }
    }
  }
}