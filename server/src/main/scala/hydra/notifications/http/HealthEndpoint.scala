package hydra.notifications.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol

object HealthEndpoint extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val infoFormat = jsonFormat1(HealthInfo)

  val routes: Route =
    path("health") {
      pathEndOrSingleSlash {
        get(complete("OK"))
      }
    }
}

case class HealthInfo(version: String)
