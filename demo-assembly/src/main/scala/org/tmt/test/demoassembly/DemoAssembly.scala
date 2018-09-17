package org.tmt.test.demoassembly

import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.command.messages.TopLevelActorMessage
import csw.command.models.matchers.DemandMatcherAll
import csw.command.scaladsl.CommandService
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.location.api.models.{AkkaLocation, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.params.commands.CommandIssue.{MissingKeyIssue, OtherIssue, UnresolvedLocationsIssue, UnsupportedCommandIssue}
import csw.params.commands.CommandResponse.Error
import csw.params.commands.{CommandName, CommandResponse, ControlCommand, Setup}
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.Prefix
import csw.params.core.states.{CurrentState, DemandState, StateName}
import csw.params.events.{EventKey, EventName, SystemEvent}
import org.tmt.test.demohcd.FilterHcd._
import org.tmt.test.demohcd.DisperserHcd._

import scala.concurrent.duration._
import scala.async.Async.async
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

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

  val filterNameKey: Key[String] = KeyType.StringKey.make("filterName")

  val filterEventName = EventName("filterEvent")

  val filterEventKey = EventKey(filterPrefix, filterEventName)

  val disperserNameKey: Key[String] = KeyType.StringKey.make("disperserName")

  val disperserEventName = EventName("disperserEvent")

  val disperserEventKey = EventKey(disperserPrefix, disperserEventName)

  // XXX TODO: Get these from the config service?
  val filters = List("None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308", "BadFilter")
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

  implicit val ec: ExecutionContextExecutor             = ctx.executionContext
  implicit val timeout: Timeout                         = 15.seconds
  private val log                                       = loggerFactory.getLogger
  private var maybeFilterHcd: Option[CommandService]    = None
  private var maybeDisperserHcd: Option[CommandService] = None
  private val eventPublisher                            = cswCtx.eventService.defaultPublisher

  override def initialize(): Future[Unit] = async {
    log.debug("Initialize called")
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"onLocationTrackingEvent called: $trackingEvent")
    trackingEvent match {
      case LocationUpdated(location) =>
        val loc = location.asInstanceOf[AkkaLocation]
        loc.connection match {
          case `filterConnection` =>
            val filterHcd = new CommandService(loc)(ctx.system)
            maybeFilterHcd = Some(filterHcd)
            filterHcd.subscribeOnlyCurrentState(Set(filterStateName), filterStateChanged)
          case `disperserConnection` =>
            val disperserHcd = new CommandService(loc)(ctx.system)
            maybeDisperserHcd = Some(disperserHcd)
            disperserHcd.subscribeOnlyCurrentState(Set(disperserStateName), disperserStateChanged)
          case x =>
            log.error(s"Unexpected location received: $x")
        }
      case LocationRemoved(connection) =>
        connection.componentId.name match {
          case "FilterHcd" =>
            maybeFilterHcd = None
          case "DisperserHcd" =>
            maybeDisperserHcd = None
          case x =>
            log.error(s"Unexpected location removed: $x")
        }
    }
  }

  private def filterStateChanged(cs: CurrentState): Unit = {
    val value = cs.get(filterKey).head.head
    eventPublisher.publish(SystemEvent(filterEventKey.source, filterEventKey.eventName).add(filterNameKey.set(filters(value))))
  }

  private def disperserStateChanged(cs: CurrentState): Unit = {
    val value = cs.get(disperserKey).head.head
    eventPublisher.publish(
      SystemEvent(disperserEventKey.source, disperserEventKey.eventName).add(disperserNameKey.set(dispersers(value)))
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
            CommandResponse.Invalid(controlCommand.runId, UnresolvedLocationsIssue(s"Missing $name HCD"))
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
      validateCommand(controlCommand, "filter", maybeFilterHcd, filterNameKey, filters),
      validateCommand(controlCommand, "disperser", maybeDisperserHcd, disperserNameKey, dispersers)
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

  private def onSubmit(controlCommand: Setup,
                       names: List[String],
                       prefix: Prefix,
                       currentStateName: StateName,
                       commandName: CommandName,
                       logName: String,
                       maybeHcd: Option[CommandService],
                       key: Key[String],
                       hcdKey: Key[Int]): Unit = {

    maybeHcd.foreach { hcd =>
      controlCommand.get(key).foreach { param =>
        // convert String name passed to assembly to Int encoder value required by HCD
        val target = names.indexOf(param.head)
        val setup  = Setup(componentInfo.prefix, commandName, controlCommand.maybeObsId).add(hcdKey.set(target))
        commandResponseManager.addSubCommand(controlCommand.runId, setup.runId)
        val demandMatcher = DemandMatcherAll(DemandState(prefix, currentStateName).add(hcdKey.set(target)), timeout)
        val response      = hcd.onewayAndMatch(setup, demandMatcher)
        response.onComplete {
          case Success(commandResponse) =>
            log.info(s"Set $logName reponded with $commandResponse")
            commandResponseManager.updateSubCommand(setup.runId, commandResponse)
          case Failure(ex) =>
            log.error(s"Set $logName error", ex = ex)
            commandResponseManager.updateSubCommand(setup.runId, Error(setup.runId, ex.toString))
        }
      }
    }
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    log.debug(s"onSubmit called: $controlCommand")

    controlCommand match {
      case s @ Setup(_, _, `demoCmd`, _, _) =>
        onSubmit(s, filters, filterPrefix, filterStateName, filterCmd, "filter", maybeFilterHcd, filterNameKey, filterKey)
        onSubmit(s,
                 dispersers,
                 disperserPrefix,
                 disperserStateName,
                 disperserCmd,
                 "disperser",
                 maybeDisperserHcd,
                 disperserNameKey,
                 disperserKey)

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
