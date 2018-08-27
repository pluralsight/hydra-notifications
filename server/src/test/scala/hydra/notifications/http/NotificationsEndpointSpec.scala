package hydra.notifications.http

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestKit, TestProbe}
import hydra.notifications.client.{OpsGenieNotification, SlackNotification}
import hydra.notifications.services.NotificationsSupervisor.SendNotification
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContextExecutor

class NotificationsEndpointSpec extends FlatSpec
  with Matchers
  with ScalatestRouteTest
  with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  class ParentActor(to: ActorRef) extends Actor {
    val childActor = context.actorOf(Props(new ForwardingActor(to)), "notifications_supervisor")

    override def receive: Receive = {
      case _ =>
    }
  }

  class ForwardingActor(to: ActorRef) extends Actor {
    override def receive: Receive = {
      case SendNotification(n) => to.forward(n)
    }
  }

  val listener = TestProbe()

  val notificationsSupervisor = system.actorOf(Props(new ParentActor(listener.ref)), "service")

  "The /notify/opsgenie endpoint" should
    "create and send an OpsGenieNotification" in {

    val route = new NotificationsEndpoint().route

    val request = Post("/notify/opsgenie?alias=scary_barry&team=team_awesome&tags=tag1,tag2&entity=da_entity&user=chunky_munkey")
      .withEntity("""OH NOES OPSGENIE PLS HALP!""".stripMargin)

    request ~> route ~> check {
      listener.expectMsgType[OpsGenieNotification]
    }
  }

  "The /notify/slack endpoint" should
    "create and send a Slack" in {

    val route = new NotificationsEndpoint().route

    val request = Post("/notify/slack?channel=test_channel")
      .withEntity("""OH NOES SLACK PLS HALP!""".stripMargin)

    request ~> route ~> check {
      listener.expectMsgType[SlackNotification]
    }
  }
}
