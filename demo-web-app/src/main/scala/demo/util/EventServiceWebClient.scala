package demo.util
import csw.params.events.Event
import org.scalajs.dom.EventSource
import play.api.libs.json.Json
import tmt.ocs.codecs.AssemblyJsonSupport

// XXX TODO: Move this to esw-prototype
object EventServiceWebClient extends AssemblyJsonSupport {
  val baseUrl = "http://localhost:9090/events/subscribe"

  /**
   * Subscribe to events for the given subsystem and optionally component and event name.
   * @param subsystem the subsystem sending the events
   * @param component optional name of a component in the subsystem
   * @param event optional name of an event sent by the component
   * @param handler a handler function that receives an event
   */
  def subscribeToEvents(subsystem: String, component: Option[String] = None, event: Option[String] = None)(
      handler: Event => Unit
  ): Unit = {
    val componentAttr = component.map(c => s"component=$c")
    val eventAttr     = event.map(e => s"event=$e")
    val attrs         = List(componentAttr, eventAttr).flatten.mkString("&")
    val attrStr       = if (attrs.isEmpty) "" else s"?$attrs"
    val url           = s"$baseUrl/$subsystem$attrStr"
    val client        = new EventSource(url)
    client.onmessage = { x =>
      val data = x.data.toString
      if (data.nonEmpty) {
        val event = Json.parse(data).as[Event]
        handler(event)
      }
    }
  }

}
