package demo

import com.github.ahnfelt.react4s._
import csw.messages.commands.{CommandName, Setup}
import csw.messages.events.{Event, EventName, SystemEvent}
import csw.messages.params.generics.{Key, KeyType}
import csw.messages.params.models.{ObsId, Prefix}
import org.scalajs.dom.EventSource
import play.api.libs.json.Json
import tmt.ocs.WebClients
import tmt.ocs.codecs.AssemblyJsonSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object MainComponent {
  // XXX TODO: Get these values dynamically from the HCDs?
  val filters                = List("None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308")
  val filterKey: Key[String] = KeyType.StringKey.make("filter")
  val filterEventName        = EventName("filterEvent")

  val dispersers =
    List("Mirror", "B1200_G5301", "R831_G5302", "B600_G5303", "B600_G5307", "R600_G5304", "R400_G5305", "R150_G5306")
  val disperserKey: Key[String] = KeyType.StringKey.make("disperser")
  val disperserEventName        = EventName("disperserEvent")

  val assemblyName = "DemoAssembly"

  // Name of command sent to this assembly to set filter and disperser values
  val demoCmd = CommandName("demo")

  // For callers: Must match config file
  val demoPrefix = Prefix("test.demo")

  val obsId = ObsId("2023-Q22-4-33")

  val titleStr = "CSW Filter Wheel Demo"
}

case class MainComponent() extends Component[NoEmit] with AssemblyJsonSupport {
  import MainComponent._

  private val currentFilter    = State(filters.head)
  private val currentDisperser = State(dispersers.head)

  private val title = E.div(A.className("row"), E.div(A.className("col s6  teal lighten-2"), Text(titleStr)))

  subscribeToEvents()

  override def render(get: Get): Element = {
    val filterComponent =
      Component(FormComboBox, "Filter", filters, get(currentFilter))
        .withHandler(s => itemSelected("Filter", s))

    val disperserComponent =
      Component(FormComboBox, "Disperser", dispersers, get(currentDisperser))
        .withHandler(s => itemSelected("Disperser", s))

    E.div(
      A.className("container"),
      title,
      filterComponent,
      disperserComponent
    )
  }

  private def itemSelected(name: String, value: String): Unit = {
    val assemblyClient = WebClients.assemblyCommandClient(assemblyName)
    val setup = if (name == "Filter") {
      Setup(demoPrefix, demoCmd, Some(obsId)).add(filterKey.set(value))
    } else {
      Setup(demoPrefix, demoCmd, Some(obsId)).add(disperserKey.set(value))
    }

    assemblyClient.submit(setup).onComplete {
      case Success(response) =>
        println(s"\nResponse $response")
      case Failure(ex) =>
        ex.printStackTrace()
        println(s"\nResponse $ex")
    }

  }

  // XXX TODO: Add utility for subscribing to events
  private def subscribeToEvents(): Unit = {
    val client = new EventSource("http://localhost:9090/events/subscribe/test")
    client.onmessage = { x =>
      val data = x.data.toString
      if (data.nonEmpty) {
        Json.parse(data).as[Event] match {
          case event: SystemEvent =>
            println(s"Received event: $event")
            if (event.eventName == filterEventName) {
              val filter = event.get(filterKey).head.head
              println(s"Selected filter from event: $filter")
              currentFilter.set(filter)
            } else if (event.eventName == disperserEventName) {
              val disperser = event.get(disperserKey).head.head
              println(s"Selected disperser from event: $disperser")
              currentDisperser.set(disperser)
            }
          case event =>
            println(s"Received unexpected event: $event")
        }
      }
    }
  }
}
