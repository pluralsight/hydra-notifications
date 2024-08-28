package hydra.notifications.http

import akka.actor.ActorRef
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.{TestActor, TestProbe}
import hydra.notifications.NotificationSent
import hydra.notifications.client.OpsGenieNotification
import hydra.notifications.services.NotificationsSupervisor.SendNotification
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpecLike, Matchers}

import scala.concurrent.duration._

class NotificationsRouteSpec extends FunSpecLike with Matchers with ScalatestRouteTest with MockFactory {

  class TestNotificationsEndpoint(supervisor: ActorRef) extends NotificationsEndpoint(supervisor)

  describe("The opsgenie notifications route") {
    it("should filter out the non-csv 'description' key from the details map and set it individually in the OpsGenieNotification") {

      val supervisorProbe = TestProbe()

      // Immediately send a response when receiving a message
      supervisorProbe.setAutoPilot((sender, msg) => {
        sender ! NotificationSent("Mocked notification sent")
        TestActor.KeepRunning
      })

      val testEndpoint = new TestNotificationsEndpoint(supervisorProbe.ref)

      val jsonPayload =
        """
          |{
          |  "level": "Error",
          |  "message": "Alert Message",
          |  "properties": {
          |    "description": "This is a test description",
          |    "key1": "value1",
          |    "key2": "value2"
          |  },
          |  "stackTrace": "",
          |  "timestamp": "2024-08-12 02:08:33"
          |}
          |""".stripMargin

      Post("/notify/opsgenie?alias=scary_barry&team=team_awesome&tags=tag1,tag2&entity=da_entity&user=chunky_munkey&priority=P1",
        jsonPayload) ~> testEndpoint.combinedRoute(supervisorProbe.ref) ~> check {
        // Expect the message to be a SendNotification containing OpsGenieNotification
        val sendNotification = supervisorProbe.expectMsgType[SendNotification]
        val notification = sendNotification.notification.asInstanceOf[OpsGenieNotification]

        // Verify that the 'description' key is not present in the details map
        notification.details shouldBe Some(Map("key1" -> "value1", "key2" -> "value2"))

        // Verify that the 'description' field is set separately
        notification.description shouldBe Some("This is a test description")

        // Verify other fields
        notification.priority shouldBe "P1"
        notification.alias shouldBe "scary_barry"
        notification.team shouldBe "team_awesome"
        notification.tags should contain allOf("tag1", "tag2")
      }
    }

    it("should retain the csv 'description' value and set it in OpsGenieNotification with the corresponding topic-wise html in details") {

      val supervisorProbe = TestProbe()

      // Immediately send a response when receiving a message
      supervisorProbe.setAutoPilot((sender, msg) => {
        sender ! NotificationSent("Mocked notification sent")
        TestActor.KeepRunning
      })

      val testEndpoint = new TestNotificationsEndpoint(supervisorProbe.ref)
      val descriptionCsv =
        """Topics,JobId,ConsumerGroupName,StartTime,TotalOffsetLag,LagPercentage,LargestOffset\ndvs.test.non-critical,1b70054f-c560-3251-8564-92e81455f213,dvs-non-critical1,2024-08-14T14:16:39.406+05:30,0,0.0,9\ndvs.test.critical,a971e760-f985-3e7a-a774-4367af885fda,dvs-critical3,2024-08-14T14:23:47.380+05:30,0,0.0,9""".stripMargin
      val jsonPayload =
        s"""
          |{
          |  "level": "Error",
          |  "message": "Alert Message",
          |  "properties": {
          |    "description": \"$descriptionCsv\",
          |    "key1": "value1",
          |    "key2": "value2"
          |  },
          |  "stackTrace": "",
          |  "timestamp": "2024-08-12 02:08:33"
          |}
          |""".stripMargin

      Post("/notify/opsgenie?alias=scary_barry&team=team_awesome&tags=tag1,tag2&entity=da_entity&user=chunky_munkey&priority=P1",
        jsonPayload) ~> testEndpoint.combinedRoute(supervisorProbe.ref) ~> check {
        // Expect the message to be a SendNotification containing OpsGenieNotification
        val sendNotification = supervisorProbe.expectMsgType[SendNotification]
        val notification = sendNotification.notification.asInstanceOf[OpsGenieNotification]

        // Verify that the 'description' key with html value is present in the details map
        notification.details shouldBe Some(
          Map(
            "key1" -> "value1",
            "key2" -> "value2",
            "dvs.test.critical" ->
              """
                |<table class="table table-bordered table-hover table-condensed"><thead><tr><th>Topics</th><th>JobId</th><th>ConsumerGroupName</th><th>StartTime</th><th>TotalOffsetLag</th><th>LagPercentage</th><th>LargestOffset</th></tr></thead><tbody><tr><td>dvs.test.critical</td><td>a971e760-f985-3e7a-a774-4367af885fda</td><td>dvs-critical3</td><td>2024-08-14T14:23:47.380+05:30</td><td align="right">0</td><td align="right">0.0</td><td align="right">9</td></tr></tbody></table>
                |""".stripMargin.trim,
            "dvs.test.non-critical" ->
              """
                |<table class="table table-bordered table-hover table-condensed"><thead><tr><th>Topics</th><th>JobId</th><th>ConsumerGroupName</th><th>StartTime</th><th>TotalOffsetLag</th><th>LagPercentage</th><th>LargestOffset</th></tr></thead><tbody><tr><td>dvs.test.non-critical</td><td>1b70054f-c560-3251-8564-92e81455f213</td><td>dvs-non-critical1</td><td>2024-08-14T14:16:39.406+05:30</td><td align="right">0</td><td align="right">0.0</td><td align="right">9</td></tr></tbody></table>
                |""".stripMargin.trim
          )
        )

        // Verify that the 'description' field with csv value is set separately
        notification.description shouldBe Some(
          """
            |Topics,JobId,ConsumerGroupName,StartTime,TotalOffsetLag,LagPercentage,LargestOffset
            |dvs.test.non-critical,1b70054f-c560-3251-8564-92e81455f213,dvs-non-critical1,2024-08-14T14:16:39.406+05:30,0,0.0,9
            |dvs.test.critical,a971e760-f985-3e7a-a774-4367af885fda,dvs-critical3,2024-08-14T14:23:47.380+05:30,0,0.0,9
            |""".stripMargin.trim)

        // Verify other fields
        notification.priority shouldBe "P1"
        notification.alias shouldBe "scary_barry"
        notification.team shouldBe "team_awesome"
        notification.tags should contain allOf("tag1", "tag2")
      }
    }

    it("should transform any key with csv value to html in details field of OpsGenieNotification") {

      val supervisorProbe = TestProbe()

      // Immediately send a response when receiving a message
      supervisorProbe.setAutoPilot((sender, msg) => {
        sender ! NotificationSent("Mocked notification sent")
        TestActor.KeepRunning
      })

      val testEndpoint = new TestNotificationsEndpoint(supervisorProbe.ref)
      val someCsvField =
        """Topics,JobId,ConsumerGroupName,StartTime,TotalOffsetLag,LagPercentage,LargestOffset\ndvs.test.critical,1b70054f-c560-3251-8564-92e81455f213,dvs-critical1,2024-08-14T14:16:39.406+05:30,0,0.0,9\ndvs.test.critical,a971e760-f985-3e7a-a774-4367af885fda,dvs-critical3,2024-08-14T14:23:47.380+05:30,0,0.0,9""".stripMargin
      val jsonPayload =
        s"""
          |{
          |  "level": "Error",
          |  "message": "Alert Message",
          |  "properties": {
          |    "someCsvField": \"$someCsvField\",
          |    "key1": "value1",
          |    "key2": "value2"
          |  },
          |  "stackTrace": "",
          |  "timestamp": "2024-08-12 02:08:33"
          |}
          |""".stripMargin

      Post("/notify/opsgenie?alias=scary_barry&team=team_awesome&tags=tag1,tag2&entity=da_entity&user=chunky_munkey&priority=P1",
        jsonPayload) ~> testEndpoint.combinedRoute(supervisorProbe.ref) ~> check {
        // Expect the message to be a SendNotification containing OpsGenieNotification
        val sendNotification = supervisorProbe.expectMsgType[SendNotification]
        val notification = sendNotification.notification.asInstanceOf[OpsGenieNotification]

        // Verify that the 'description' key with html value is present in the details map
        notification.details shouldBe Some(
          Map(
            "key1" -> "value1",
            "key2" -> "value2",
            "someCsvField" ->
              """
                |<table class="table table-bordered table-hover table-condensed"><thead><tr><th>Topics</th><th>JobId</th><th>ConsumerGroupName</th><th>StartTime</th><th>TotalOffsetLag</th><th>LagPercentage</th><th>LargestOffset</th></tr></thead><tbody><tr><td>dvs.test.critical</td><td>1b70054f-c560-3251-8564-92e81455f213</td><td>dvs-critical1</td><td>2024-08-14T14:16:39.406+05:30</td><td align="right">0</td><td align="right">0.0</td><td align="right">9</td></tr><tr><td>dvs.test.critical</td><td>a971e760-f985-3e7a-a774-4367af885fda</td><td>dvs-critical3</td><td>2024-08-14T14:23:47.380+05:30</td><td align="right">0</td><td align="right">0.0</td><td align="right">9</td></tr></tbody></table>
                |""".stripMargin.trim
          )
        )

        // Verify that the 'description' field is missing
        notification.description shouldBe None

        // Verify other fields
        notification.priority shouldBe "P1"
        notification.alias shouldBe "scary_barry"
        notification.team shouldBe "team_awesome"
        notification.tags should contain allOf("tag1", "tag2")
      }
    }
  }
}
