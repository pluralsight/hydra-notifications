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

import hydra.notifications.HydraNotification
import hydra.notifications.services.{OpsGenieNotification, SlackNotification}
import org.scalatest.{FunSpecLike, Matchers}

class NotificationsFormatSpec extends Matchers with FunSpecLike {

  import NotificationsFormat._
  import spray.json._

  describe("The Notifications Spray Format") {
    it("should marshall a slack notification") {
      val sn: HydraNotification = SlackNotification("channel", "an error occured")
      sn.toJson shouldBe """{"channel":"channel","message":"an error occured","service":"slack"}""".parseJson
    }

    it("should marshall an opsgenie notification") {
      val sn: HydraNotification = OpsGenieNotification("message", "alias", Some("description"), Some("note"),
        "team", Seq("tag1", "tag2"), "entity", Some("source"), "user")
      val expected =
        """{"source":"source","description":"description","tags":["tag1","tag2"],"service":"opsgenie",
          |"alias":"alias","note":"note","team":"team","entity":"entity",
          |"message":"message","user":"user"}""".stripMargin.parseJson
      sn.toJson shouldBe expected
    }
  }

  it("errors on unknown format") {
    intercept[DeserializationException] {
      """{"channel":"channel","message":"an error occured",
        |"service":"unknown"}""".stripMargin.parseJson.convertTo[HydraNotification]
    }
  }
}
