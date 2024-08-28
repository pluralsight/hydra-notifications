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
        """Topics,Job Id,Consumer Group Name,Start Time,Total Offset Lag,Lag Percentage,Largest Offset\ntech.identity.v4.User,a5397917-e717-334f-8119-f49f689a891a,communications---tech.identity.v4.User---v4,2023-09-12T12:34:38.309Z,15104,1.2045934696423146,1253867\nskills.plans.v1.Learner,d473d0a5-4036-3720-a8a9-2b5db748a4bb,labs-team-alpha-skills.plans.v1.Learner-staging-2024-01-26-1706287647,2024-01-26T16:47:34.384Z,8375,0.17778523119509887,4710740\nexp.identity.User,8068c088-fb97-361e-9371-29ec66fb84e2,ExploreV2---exp.identity.User,2024-01-04T10:16:06.843Z,1963690,100.0,1963690\nskills.plans.v1.Learner,d2f1cdbf-0f98-319b-b05a-3466a390227a,communications---skills.plans.v1.Learner---v1,2023-09-04T11:04:30.780Z,8375,0.17778523119509887,4710740\ntech.identity.v4.User,b487005d-bc61-3d35-95af-ef6df34314b4,roleiq-learner---tech.identity.v4.User,2023-10-23T12:01:42.783Z,1253867,100.0,1253867\nexp.identity.User,65dd7109-bd78-3247-bb92-30a1ad116f44,curation---exp.identity.User,2023-09-06T17:28:55.176Z,1963690,100.0,1963690\ntech.identity.v4.User,06a459df-e83e-3b7c-8913-4fd9c9dac773,redirects---tech.identity.v4.User,2023-09-07T01:02:23.295Z,5667,0.45196181094167087,1253867""".stripMargin
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
            "description" ->
              """
                |Topics,Job Id,Consumer Group Name,Start Time,Total Offset Lag,Lag Percentage,Largest Offset
                |tech.identity.v4.User,a5397917-e717-334f-8119-f49f689a891a,communications---tech.identity.v4.User---v4,2023-09-12T12:34:38.309Z,15104,1.2045934696423146,1253867
                |tech.identity.v4.User,b487005d-bc61-3d35-95af-ef6df34314b4,roleiq-learner---tech.identity.v4.User,2023-10-23T12:01:42.783Z,1253867,100.0,1253867
                |tech.identity.v4.User,06a459df-e83e-3b7c-8913-4fd9c9dac773,redirects---tech.identity.v4.User,2023-09-07T01:02:23.295Z,5667,0.45196181094167087,1253867
                |skills.plans.v1.Learner,d473d0a5-4036-3720-a8a9-2b5db748a4bb,labs-team-alpha-skills.plans.v1.Learner-staging-2024-01-26-1706287647,2024-01-26T16:47:34.384Z,8375,0.17778523119509887,4710740
                |skills.plans.v1.Learner,d2f1cdbf-0f98-319b-b05a-3466a390227a,communications---skills.plans.v1.Learner---v1,2023-09-04T11:04:30.780Z,8375,0.17778523119509887,4710740
                |exp.identity.User,8068c088-fb97-361e-9371-29ec66fb84e2,ExploreV2---exp.identity.User,2024-01-04T10:16:06.843Z,1963690,100.0,1963690
                |exp.identity.User,65dd7109-bd78-3247-bb92-30a1ad116f44,curation---exp.identity.User,2023-09-06T17:28:55.176Z,1963690,100.0,1963690
                |""".stripMargin.trim
          )
        )

        // Verify that the 'description' field with csv value is set separately
        notification.description shouldBe Some(
          """
            |<table class="table table-bordered table-hover table-condensed"><thead><tr><th>Topics</th><th>Job Id</th><th>Consumer Group Name</th><th>Start Time</th><th>Total Offset Lag</th><th>Lag Percentage</th><th>Largest Offset</th></tr></thead><tbody><tr><td>tech.identity.v4.User</td><td>a5397917-e717-334f-8119-f49f689a891a</td><td>communications---tech.identity.v4.User---v4</td><td>2023-09-12T12:34:38.309Z</td><td align="right">15104</td><td align="right">1.2045934696423146</td><td align="right">1253867</td></tr><tr><td>tech.identity.v4.User</td><td>b487005d-bc61-3d35-95af-ef6df34314b4</td><td>roleiq-learner---tech.identity.v4.User</td><td>2023-10-23T12:01:42.783Z</td><td align="right">1253867</td><td align="right">100.0</td><td align="right">1253867</td></tr><tr><td>tech.identity.v4.User</td><td>06a459df-e83e-3b7c-8913-4fd9c9dac773</td><td>redirects---tech.identity.v4.User</td><td>2023-09-07T01:02:23.295Z</td><td align="right">5667</td><td align="right">0.45196181094167087</td><td align="right">1253867</td></tr><tr><td>skills.plans.v1.Learner</td><td>d473d0a5-4036-3720-a8a9-2b5db748a4bb</td><td>labs-team-alpha-skills.plans.v1.Learner-staging-2024-01-26-1706287647</td><td>2024-01-26T16:47:34.384Z</td><td align="right">8375</td><td align="right">0.17778523119509887</td><td align="right">4710740</td></tr><tr><td>skills.plans.v1.Learner</td><td>d2f1cdbf-0f98-319b-b05a-3466a390227a</td><td>communications---skills.plans.v1.Learner---v1</td><td>2023-09-04T11:04:30.780Z</td><td align="right">8375</td><td align="right">0.17778523119509887</td><td align="right">4710740</td></tr><tr><td>exp.identity.User</td><td>8068c088-fb97-361e-9371-29ec66fb84e2</td><td>ExploreV2---exp.identity.User</td><td>2024-01-04T10:16:06.843Z</td><td align="right">1963690</td><td align="right">100.0</td><td align="right">1963690</td></tr><tr><td>exp.identity.User</td><td>65dd7109-bd78-3247-bb92-30a1ad116f44</td><td>curation---exp.identity.User</td><td>2023-09-06T17:28:55.176Z</td><td align="right">1963690</td><td align="right">100.0</td><td align="right">1963690</td></tr></tbody></table>
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
