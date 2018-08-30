package hydra.notifications.client

abstract class HydraNotification {
  def service: String

  def message: String
}

case class SlackNotification(channel: String, message: String) extends HydraNotification {
  override val service = "slack"
}


case class OpsGenieNotification(message: String,
                                alias: String,
                                description: Option[String],
                                note: Option[String],
                                team: String,
                                tags: Seq[String],
                                entity: String,
                                source: Option[String],
                                user: String) extends HydraNotification {

  override val service = "opsgenie"
}


case class NotificationsResponse(statusCode: Int, message: String)