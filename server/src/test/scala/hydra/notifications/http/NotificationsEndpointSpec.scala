package hydra.notifications.http

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class NotificationsEndpointSpec extends FlatSpec
  with Matchers
  with ScalatestRouteTest
  with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "The notify/opsgenie/ endpoint" should
    "create "
}
