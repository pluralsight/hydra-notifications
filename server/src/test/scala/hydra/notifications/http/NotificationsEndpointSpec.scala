package hydra.notifications.http

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestKit, TestProbe}
import hydra.notifications.NotificationSent
import hydra.notifications.client.{OpsGenieNotification, SlackNotification}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContextExecutor

class NotificationsEndpointSpec extends FlatSpec
  with Matchers
  with ScalatestRouteTest
  with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  implicit val ec: ExecutionContextExecutor = system.dispatcher



  class ForwardingActor(to: ActorRef) extends Actor {
    override def receive: Receive = {
      case NotificationSent(message) =>
        println(s"OH HEY! I RECEIVED $message")
        to.forward(message)
        sender ! NotificationSent("message sent successfully")
    }
  }

  val listener = TestProbe("service")

  val testNotificationSupervisor = system.actorOf(Props(new ForwardingActor(listener.ref)))

  "The notify/opsgenie endpoint" should
    "create and send an OpsGenieNotification" in {

    val route = new NotificationsEndpoint(Some(testNotificationSupervisor)).route

    val request = Post("/notify/opsgenie?alias=scary_barry&team=team_awesome&tags=tag1,tag2&entity=da_entity&user=chunky_munkey")
      .withEntity(
        ContentTypes.`application/json`,
        """
           OH NOES OPSGENIE PLS HALP!
        """.stripMargin)

    Post("/notify/opsgenie") ~> route ~> check {
      listener.expectMsgType[OpsGenieNotification]
    }
  }

  "The notify/slack endpoint" should
    "create and send a Slack" in {

    val route = new NotificationsEndpoint(Some(testNotificationSupervisor)).route

    val request = Post("/notify/slack?channel=test_channel")
      .withEntity(
        ContentTypes.`application/json`,
        """
           OH NOES SLACK PLS HALP!
        """.stripMargin)

    request ~> route ~> check {
      listener.expectMsgType[SlackNotification]
    }
  }
}
