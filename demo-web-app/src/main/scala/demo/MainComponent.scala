package demo

import com.github.ahnfelt.react4s._
import csw.params.commands.CommandResponse.{Accepted, Completed, Error}
import csw.params.commands.{CommandName, CommandResponse, Setup}
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.{Id, ObsId, Prefix}
import csw.params.events.{EventName, SystemEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object MainComponent {
  // XXX TODO: Get these values dynamically from the HCDs?
  val filters                = List("None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308", "BadFilter")
  val filterKey: Key[String] = KeyType.StringKey.make("filter")
  val filter                 = "Filter"

  val dispersers =
    List("Mirror", "B1200_G5301", "R831_G5302", "B600_G5303", "B600_G5307", "R600_G5304", "R400_G5305", "R150_G5306")
  val disperserKey: Key[String] = KeyType.StringKey.make("disperser")
  val disperser                 = "Disperser"

  val demoEventName: EventName = EventName("demoEvent")

  val assemblyName = "DemoAssembly"

  // Name of command sent to this assembly to set filter and disperser values
  val demoCmd = CommandName("demo")

  // For callers: Must match config file
  val demoPrefix = Prefix("test.demo")

  // For testing
  val obsId = ObsId("2023-Q22-4-33")

  val titleStr = "CSW Filter Wheel Demo"
}

case class MainComponent() extends Component[NoEmit] {
  import MainComponent._

  private val currentFilter                                    = State(filters.head)
  private val currentDisperser                                 = State(dispersers.head)
  private val filterCommandResponse: State[CommandResponse]    = State(Completed(Id()))
  private val disperserCommandResponse: State[CommandResponse] = State(Completed(Id()))
  private val title                                            = E.div(A.className("row"), E.div(A.className("col s6  teal lighten-2"), Text(titleStr)))
  private val gateway                                          = new WebGateway()
  private val eventClient                                      = new EventJsClient(gateway)
  private val subsystem                                        = "test"
  private val eventStream                                      = eventClient.subscribe(subsystem)

  // Handle events
  eventStream.onNext = {
    case event: SystemEvent =>
      if (event.eventName == demoEventName) {
        val filter = event.get(filterKey).get.head
        currentFilter.set(filter)
        val disperser = event.get(disperserKey).get.head
        currentDisperser.set(disperser)
      }
    case event =>
      println(s"Received unexpected event: $event")
  }

  override def render(get: Get): Element = {
    val filterComponent =
      Component(FormComboBox, "Filter", filters, get(currentFilter), get(filterCommandResponse))
        .withHandler(s => itemSelected(filter, s))

    val disperserComponent =
      Component(FormComboBox, disperser, dispersers, get(currentDisperser), get(disperserCommandResponse))
        .withHandler(s => itemSelected(disperser, s))

    E.div(
      A.className("container"),
      title,
      filterComponent,
      disperserComponent
    )
  }

  private def commandResponseStateVariable(name: String) =
    if (name == filter) filterCommandResponse else disperserCommandResponse

  private def itemSelected(name: String, value: String): Unit = {
    val assemblyClient = WebClients.assemblyCommandClient(assemblyName)
    val setup = if (name == filter) {
      Setup(demoPrefix, demoCmd, Some(obsId)).add(filterKey.set(value))
    } else {
      Setup(demoPrefix, demoCmd, Some(obsId)).add(disperserKey.set(value))
    }

    val commandResponseState = commandResponseStateVariable(name)

    // Set state to Accepted while waiting for the final response
    commandResponseState.set(Accepted(setup.runId))

    assemblyClient.submitAndSubscribe(setup).onComplete {
      case Success(response) =>
        println(s"\nResponse $response")
        commandResponseState.set(response)
      case Failure(ex) =>
        ex.printStackTrace()
        println(s"\nResponse $ex")
        commandResponseState.set(Error(setup.runId, ex.getMessage))
    }

  }

}
