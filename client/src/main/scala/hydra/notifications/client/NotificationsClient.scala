package hydra.notifications.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri.Query
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
    val uri = notification match {
      case s: SlackNotification =>
        val query = Query("channel" -> s.channel)
        Uri("/notify/slack").withQuery(query)
      case o: OpsGenieNotification =>
        val query = Query(
          "alias" -> o.alias,
          "description" -> o.description.getOrElse(""),
          "note" -> o.note.getOrElse(""),
          "team" -> o.team,
          "tags" -> o.tags.mkString(","),
          "entity" -> o.entity,
          "source" -> o.source.getOrElse(""),
          "user" -> o.user)
        Uri("/notify/opsgenie").withQuery(query)
      case s =>
        throw new IllegalArgumentException(s"$s is not a supported notification service.")
    }
    
    Source.single(Post(uri)
      .withEntity(ContentTypes.`application/json`, notification.message))
      .via(httpClient)
      .runWith(Sink.head)
      .flatMap(r => Unmarshal(r.entity).to[NotificationsResponse])
  }

  def shutdown(): Unit = Http().shutdownAllConnectionPools()
}
