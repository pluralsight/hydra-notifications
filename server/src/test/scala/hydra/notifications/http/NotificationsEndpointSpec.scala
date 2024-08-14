package hydra.notifications.http

import akka.actor.{Actor, Props}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import hydra.notifications.NotificationSent
import hydra.notifications.services.NotificationsSupervisor.SendNotification
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContextExecutor

class NotificationsEndpointSpec extends FlatSpec
  with Matchers
  with ScalatestRouteTest
  with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  implicit val ec: ExecutionContextExecutor = system.dispatcher


  val notificationsSupervisor = system.actorOf(Props(new Actor {
    override def receive: Receive = {
      case SendNotification(svc) => sender ! NotificationSent(svc.service)
    }
  }))

  "The /notify/opsgenie endpoint" should
    "create and send an OpsGenieNotification with a simple string payload" in {

    val route = new NotificationsEndpoint(notificationsSupervisor).routes

    val request = Post("/notify/opsgenie?alias=scary_barry&team=team_awesome&tags=tag1,tag2&entity=da_entity&user=chunky_munkey&priority=P1")
      .withEntity("""OH NOES OPSGENIE PLS HALP!""".stripMargin)

    request ~> route ~> check {
      response.status.intValue() shouldBe 200
    }
  }

  it should "create and send an OpsGenieNotification with a correct json payload" in {
    val jsonPayload =
      """
        |{
        |  "level": "Error",
        |  "message": "Job has running status but hasn't received a message in a while. Lag level: 76.83%",
        |  "properties": {
        |    "host": "ipr-hydra-streams-8",
        |    "jobId": "54a6db71-b627-3041-9f54-b37957abc4x1",
        |    "applicationId": "test---dvs.test.v1.testLag",
        |    "description": "Sample description."
        |  },
        |  "stackTrace": "",
        |  "timestamp": "2024-08-12 02:08:33"
        |}
        |""".stripMargin

    val route = new NotificationsEndpoint(notificationsSupervisor).routes

    val request = Post("/notify/opsgenie?alias=scary_barry&team=team_awesome&tags=tag1,tag2&entity=da_entity&user=chunky_munkey&priority=P1")
      .withEntity(jsonPayload)

    request ~> route ~> check {
      response.status.intValue() shouldBe 200
    }
  }

  it should "throw an error for invalid json payload while creating OpsGenieNotification" in {
    val invalidJson = """{"invalid": "data"}""".stripMargin

    val route = new NotificationsEndpoint(notificationsSupervisor).routes

    val request = Post("/notify/opsgenie?alias=scary_barry&team=team_awesome&tags=tag1,tag2&entity=da_entity&user=chunky_munkey&priority=P1")
      .withEntity(invalidJson)

    request ~> route ~> check {
      response.status.intValue() shouldBe 400
    }
  }

  "The /notify/slack endpoint" should
    "create and send a Slack" in {

    val route = new NotificationsEndpoint(notificationsSupervisor).routes

    val request = Post("/notify/slack?channel=test_channel")
      .withEntity("""OH NOES SLACK PLS HALP!""".stripMargin)

    request ~> route ~> check {
      response.status.intValue() shouldBe 200
    }
  }
}
