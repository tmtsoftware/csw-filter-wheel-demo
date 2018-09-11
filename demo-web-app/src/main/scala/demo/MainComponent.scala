package demo

import com.github.ahnfelt.react4s._
import csw.messages.commands.{CommandName, Setup}
import csw.messages.params.generics.{Key, KeyType}
import csw.messages.params.models.{ObsId, Prefix}
import org.scalajs.dom.EventSource
import tmt.WebClients

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object MainComponent {
  // XXX TODO: Get these values dynamically from the HCDs?
  val filters                = List("None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308")
  val filterKey: Key[String] = KeyType.StringKey.make("filter")

  val dispersers =
    List("Mirror", "B1200_G5301", "R831_G5302", "B600_G5303", "B600_G5307", "R600_G5304", "R400_G5305", "R150_G5306")
  val disperserKey: Key[String] = KeyType.StringKey.make("disperser")

  val assemblyName = "DemoAssembly"

  // Name of command sent to this assembly to set filter and disperser values
  val demoCmd = CommandName("demo")

  // For callers: Must match config file
  val demoPrefix = Prefix("test.demo")

  private val obsId = ObsId("2023-Q22-4-33")
}

case class MainComponent() extends Component[NoEmit] {
  import MainComponent._

//  // XXX temp: Timer handle, used until event service API for Scala.js is ready
//  var interval: js.UndefOr[js.timers.SetIntervalHandle] = js.undefined

  subscribeToEvents()

  override def render(get: Get): Element = {

    E.div(
      A.className("container"),
      E.div(A.className("row teal lighten-2"), E.div(A.className("col s12"), Text("CSW Filter Wheel Demo"))),
      E.div(A.className("row"), E.div(Component(FormComboBox, "Filter", filters).withHandler(s => itemSelected("Filter", s)))),
      E.div(A.className("row"),
            E.div(Component(FormComboBox, "Disperser", dispersers).withHandler(s => itemSelected("Disperser", s))))
    )
  }

  private def itemSelected(name: String, value: String): Unit = {
    println(s"XXX Selected $name: $value")

    val assemblyClient = WebClients.assemblyCommandClient(assemblyName)
    val setup = if (name == "Filter") {
      Setup(demoPrefix, demoCmd, Some(obsId)).add(filterKey.set(value))
    } else {
      Setup(demoPrefix, demoCmd, Some(obsId)).add(disperserKey.set(value))
    }

//    // XXX temp timer
//    interval = js.timers.setInterval(1000)(tick.runNow())

    assemblyClient.submit(setup).onComplete {
      case Success(response) => println(s"\nResponse $response")
      case Failure(ex) =>
        ex.printStackTrace()
        println(s"\nResponse $ex")
    }

  }

  private def subscribeToEvents(): Unit = {
    val client = new EventSource("http://localhost:9090/events/subscribe/subsystem/test")
    client.onmessage = { x =>
      println(s"Received event: ${x.data}")
    }
  }
}
