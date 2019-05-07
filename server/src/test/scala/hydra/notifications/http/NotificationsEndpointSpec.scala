package hydra.notifications.http

import akka.actor.{Actor, Props}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import hydra.notifications.NotificationSent
import hydra.notifications.client.HydraNotification
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
    "create and send an OpsGenieNotification" in {

    val route = new NotificationsEndpoint(notificationsSupervisor).routes

    val request = Post("/notify/opsgenie?alias=scary_barry&team=team_awesome&tags=tag1,tag2&entity=da_entity&user=chunky_munkey")
      .withEntity("""OH NOES OPSGENIE PLS HALP!""".stripMargin)

    request ~> route ~> check {
      response.status.intValue() shouldBe 200
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
