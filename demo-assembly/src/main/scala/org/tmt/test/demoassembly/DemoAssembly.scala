package org.tmt.test.demoassembly

import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.command.messages.TopLevelActorMessage
import csw.command.scaladsl.CommandService
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.params.commands.CommandIssue.{MissingKeyIssue, OtherIssue, UnresolvedLocationsIssue, UnsupportedCommandIssue}
import csw.params.commands._
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.Prefix
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{EventKey, EventName, SystemEvent}
import csw.proto.galil.io.DataRecord

import scala.concurrent.duration._
import scala.async.Async.async
import scala.concurrent.{ExecutionContextExecutor, Future}

class DemoAssemblyBehaviorFactory extends ComponentBehaviorFactory {

  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      cswServices: CswContext
  ): ComponentHandlers =
    new DemoAssemblyHandlers(ctx, cswServices)

}

object DemoAssembly {
  // Name of command sent to this assembly to set filter and disperser values
  val demoCmd = CommandName("demo")

  // For callers: Must match config file
  val demoPrefix = Prefix("test.DemoAssembly")

  val galilConnection = AkkaConnection(ComponentId("GalilHcd", ComponentType.HCD))

  val galilCurrentStateName = StateName("DataRecord")

  val referencePositionKey: Key[Int] = KeyType.IntKey.make("referencePosition")

  val filterKey: Key[String] = KeyType.StringKey.make("filter")

  val filterCmd = CommandName("setFilter")

  val demoEventName = EventName("demoEvent")

  val disperserKey: Key[String] = KeyType.StringKey.make("disperser")

  // XXX TODO: Get these from the config service?
  val filters = List("None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308")
  val dispersers =
    List("Mirror", "B1200_G5301", "R831_G5302", "B600_G5303", "B600_G5307", "R600_G5304", "R400_G5305", "R150_G5306")
}

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to DemoHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw-prod/framework.html
 */
class DemoAssemblyHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    cswCtx: CswContext
) extends ComponentHandlers(ctx, cswCtx) {
  import DemoAssembly._
  import cswCtx._

  implicit val ec: ExecutionContextExecutor         = ctx.executionContext
  implicit val timeout: Timeout                     = 15.seconds
  private val log                                   = loggerFactory.getLogger
  private var maybeGalilHcd: Option[CommandService] = None
  private val eventPublisher                        = cswCtx.eventService.defaultPublisher
  val demoEventKey                                  = EventKey(componentInfo.prefix, demoEventName)

  override def initialize(): Future[Unit] = async {
    log.debug("Initialize called")
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"onLocationTrackingEvent called: $trackingEvent")
    trackingEvent match {
      case LocationUpdated(location) =>
        val loc = location.asInstanceOf[AkkaLocation]
        loc.connection match {
          case `galilConnection` =>
            val galilHcd = new CommandService(loc)(ctx.system)
            maybeGalilHcd = Some(galilHcd)
            galilHcd.subscribeOnlyCurrentState(Set(galilCurrentStateName), galilStateChanged)
            val results =
              List(GalilHelper.init(galilHcd, componentInfo.prefix, 'A'), GalilHelper.init(galilHcd, componentInfo.prefix, 'B'))
            results.foreach { r =>
              if (r.resultType != CommandResultType.Positive) log.error(s"Error initializing GalilHcd: $r")
            }
          case x =>
            log.error(s"Unexpected location received: $x")
        }
      case LocationRemoved(connection) =>
        connection.componentId.name match {
          case "GalilHcd" =>
            maybeGalilHcd = None
          case x =>
            log.error(s"Unexpected location removed: $x")
        }
    }
  }

  private def galilStateChanged(cs: CurrentState): Unit = {
    val dataRecord    = DataRecord(Result(componentInfo.prefix, cs.paramSet))
    val filterPos     = dataRecord.axisStatuses(0).referencePosition
    val filterIndex   = (filterPos / 25) % filters.size
    val currentFilter = filters(filterIndex)

    val disperserPos     = dataRecord.axisStatuses(1).referencePosition
    val disperserIndex   = (disperserPos / 25) % dispersers.size
    val currentDisperser = dispersers(disperserIndex)

    log.info(s"Current state: filter: $currentFilter, disperser: $currentDisperser")

    eventPublisher.publish(
      SystemEvent(demoEventKey.source, demoEventKey.eventName)
        .add(filterKey.set(currentFilter))
        .add(disperserKey.set(currentDisperser))
    )
  }

  private def validateCommand(controlCommand: ControlCommand,
                              name: String,
                              maybeHcd: Option[CommandService],
                              key: Key[String],
                              choices: List[String]): Option[CommandResponse] = {
    controlCommand match {
      case s @ Setup(_, _, `demoCmd`, _, _) =>
        s.get(key).map { param =>
          val value = param.head
          if (maybeHcd.isEmpty) {
            CommandResponse.Invalid(controlCommand.runId, UnresolvedLocationsIssue(s"Missing Galil HCD"))
          } else {
            if (!choices.contains(value))
              CommandResponse.Invalid(controlCommand.runId, OtherIssue(s"Unknown $name: $value"))
            else CommandResponse.Accepted(controlCommand.runId)
          }
        }

      case x =>
        Some(CommandResponse.Invalid(controlCommand.runId, UnsupportedCommandIssue(s"Unsupported command: $x")))
    }
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    val responses = List(
      validateCommand(controlCommand, "filter", maybeGalilHcd, filterKey, filters),
      validateCommand(controlCommand, "disperser", maybeGalilHcd, disperserKey, dispersers)
    ).flatten
    if (responses.isEmpty)
      CommandResponse.Invalid(controlCommand.runId, MissingKeyIssue(s"Missing required filter or disperser key"))
    else {
      val invalidResponses = responses.filter(!_.isInstanceOf[CommandResponse.Accepted])
      if (invalidResponses.nonEmpty)
        invalidResponses.head
      else CommandResponse.Accepted(controlCommand.runId)
    }
  }

  private def handleSubmit(controlCommand: Setup,
                           axis: Char,
                           names: List[String],
                           maybeHcd: Option[CommandService],
                           key: Key[String]): Unit = {

    maybeHcd.foreach { hcd =>
      controlCommand.get(key).foreach { param =>
        // convert String name passed to assembly to Int encoder value (index in list of names...)
        val target = names.indexOf(param.head)
        GalilHelper
          .setPosition(hcd, componentInfo.prefix, controlCommand.maybeObsId, axis, target, commandResponseManager, controlCommand)
      }
    }
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    log.debug(s"onSubmit called: $controlCommand")

    controlCommand match {
      case s @ Setup(_, _, `demoCmd`, _, _) =>
        handleSubmit(s, 'A', filters, maybeGalilHcd, filterKey)
        handleSubmit(s, 'B', dispersers, maybeGalilHcd, disperserKey)

      case x =>
        // Should not happen
        log.error(s"Unsupported command received: $x")
    }
  }

  override def onOneway(controlCommand: ControlCommand): Unit = {
    log.debug("onOneway called")

  }

  override def onShutdown(): Future[Unit] = async {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

}
