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

  implicit val timeout: RouteTestTimeout = RouteTestTimeout(10.seconds)

  class TestNotificationsEndpoint(supervisor: ActorRef) extends NotificationsEndpoint(supervisor)

  describe("The opsgenie notifications route") {
    it("should filter out the 'description' key from the details map and set it individually in the OpsGenieNotification") {

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
  }
}
