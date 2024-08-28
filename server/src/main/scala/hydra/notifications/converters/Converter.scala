package hydra.notifications.converters

trait Converter

object Converter {

  def escape(value: String): String = {
    value
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\b", "\\b")
      .replace("\f", "\\f")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")
  }

  def unescape(value: String): String = {
    value
      .replace("\\\"", "\"")
      .replace("\\\\", "\\")
      .replace("\\b", "\b")
      .replace("\\f", "\f")
      .replace("\\n", "\n")
      .replace("\\r", "\r")
      .replace("\\t", "\t")
  }
}
