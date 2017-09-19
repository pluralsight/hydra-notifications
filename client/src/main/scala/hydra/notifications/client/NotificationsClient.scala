package hydra.notifications.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future

class NotificationsClient(host: String, port: Int)(implicit sys: ActorSystem) extends SprayJsonSupport {

  import NotificationsFormat._

  private val httpClient = Http().outgoingConnection(host, port = port)

  private implicit val ec = sys.dispatcher

  private implicit val materializer = ActorMaterializer()

  def postNotification(notification: HydraNotification): Future[NotificationsResponse] = {
    Marshal(notification).to[RequestEntity].flatMap { entity =>
      val post = Post("/").withEntity(entity)
      Source.single(post).via(httpClient).runWith(Sink.head)
        .flatMap(r => Unmarshal(r.entity).to[NotificationsResponse])
    }

  }

  def shutdown(): Unit = Http().shutdownAllConnectionPools()
}
