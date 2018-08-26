package org.tmt.test.demoassembly

import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.framework.CurrentStatePublisher
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.commands.CommandIssue.{MissingKeyIssue, OtherIssue, UnresolvedLocationsIssue, UnsupportedCommandIssue}
import csw.messages.commands.CommandResponse.Error
import csw.messages.commands.matchers.DemandMatcherAll
import csw.messages.commands.{CommandName, CommandResponse, ControlCommand, Setup}
import csw.messages.framework.ComponentInfo
import csw.messages.location.{AkkaLocation, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.messages.params.models.Prefix
import csw.messages.params.states.DemandState
import csw.services.alarm.api.scaladsl.AlarmService
import csw.services.command.CommandResponseManager
import csw.services.command.scaladsl.CommandService
import csw.services.event.api.scaladsl.EventService
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory
import org.tmt.test.demohcd.FilterHcd._
import org.tmt.test.demohcd.DisperserHcd._

import scala.concurrent.duration._
import scala.async.Async.async
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class DemoAssemblyBehaviorFactory extends ComponentBehaviorFactory {

  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      componentInfo: ComponentInfo,
      commandResponseManager: CommandResponseManager,
      currentStatePublisher: CurrentStatePublisher,
      locationService: LocationService,
      eventService: EventService,
      alarmService: AlarmService,
      loggerFactory: LoggerFactory
  ): ComponentHandlers =
    new DemoAssemblyHandlers(ctx,
                             componentInfo,
                             commandResponseManager,
                             currentStatePublisher,
                             locationService,
                             eventService,
                             alarmService,
                             loggerFactory)

}

object DemoAssembly {
  // Name of command sent to this assembly to set filter and disperser values
  val demoCmd = CommandName("demo")

  // For callers: Must match config file
  val demoPrefix = Prefix("test.demo")
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
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    locationService: LocationService,
    eventService: EventService,
    alarmService: AlarmService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(ctx,
                              componentInfo,
                              commandResponseManager,
                              currentStatePublisher,
                              locationService,
                              eventService,
                              alarmService,
                              loggerFactory) {
  import DemoAssembly._

  implicit val ec: ExecutionContextExecutor             = ctx.executionContext
  implicit val timeout: Timeout                         = 3.seconds
  private val log                                       = loggerFactory.getLogger
  private var maybeFilterHcd: Option[CommandService]    = None
  private var maybeDisperserHcd: Option[CommandService] = None

  override def initialize(): Future[Unit] = async {
    log.debug("Initialize called")
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"onLocationTrackingEvent called: $trackingEvent")
    trackingEvent match {
      case LocationUpdated(location) =>
        val loc = location.asInstanceOf[AkkaLocation]
        loc.connection.name match {
          case "FilterHcd" =>
            maybeFilterHcd = Some(new CommandService(loc)(ctx.system))
          case "DisperserHcd" =>
            maybeDisperserHcd = Some(new CommandService(loc)(ctx.system))
        }
      case LocationRemoved(connection) =>
        connection.name match {
          case "FilterHcd" =>
            maybeFilterHcd = None
          case "DisperserHcd" =>
            maybeDisperserHcd = None
        }
    }
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    controlCommand match {
      case s @ Setup(_, _, `demoCmd`, _, _) =>
        if (maybeFilterHcd.isEmpty)
          CommandResponse.Invalid(controlCommand.runId, UnresolvedLocationsIssue("Missing filter HCD"))
        else if (maybeDisperserHcd.isEmpty)
          CommandResponse.Invalid(controlCommand.runId, UnresolvedLocationsIssue("Missing disperser HCD"))
        else if (!s.exists(filterKey)) {
          CommandResponse.Invalid(controlCommand.runId, MissingKeyIssue(s"Missing ${filterKey.keyName} key"))
        } else if (!s.exists(disperserKey)) {
          CommandResponse.Invalid(controlCommand.runId, MissingKeyIssue(s"Missing ${disperserKey.keyName} key"))
        } else {
          val filter    = s.get(filterKey).get.head
          val disperser = s.get(disperserKey).get.head
          if (!filters.contains(filter))
            CommandResponse.Invalid(controlCommand.runId, OtherIssue(s"Unknown filter: $filter"))
          else if (!dispersers.contains(disperser))
            CommandResponse.Invalid(controlCommand.runId, OtherIssue(s"Unknown disperser: $disperser"))
          else CommandResponse.Accepted(controlCommand.runId)
        }

      case x =>
        CommandResponse.Invalid(controlCommand.runId, UnsupportedCommandIssue(s"Unsupported command: $x"))
    }
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    log.debug(s"onSubmit called: $controlCommand")

    controlCommand match {
      case s @ Setup(_, _, `demoCmd`, _, _) =>
        maybeFilterHcd.foreach { filterHcd =>
          val target = s.get(filterKey).get.head
          val setup  = Setup(componentInfo.prefix, filterCmd, controlCommand.maybeObsId).add(filterKey.set(target))
          commandResponseManager.addSubCommand(s.runId, setup.runId)
          val demandMatcher = DemandMatcherAll(DemandState(filterPrefix, filterStateName).add(filterKey.set(target)), timeout)
          val response      = filterHcd.onewayAndMatch(setup, demandMatcher)
          response.onComplete {
            case Success(commandResponse) =>
              log.info(s"Set filter reponded with $commandResponse")
              commandResponseManager.updateSubCommand(setup.runId, commandResponse)
            case Failure(ex) =>
              log.error(s"Set filter error", ex = ex)
              commandResponseManager.updateSubCommand(setup.runId, Error(setup.runId, ex.toString))
          }
        }
        maybeDisperserHcd.foreach { disperserHcd =>
          val target = s.get(disperserKey).get.head
          val setup  = Setup(componentInfo.prefix, disperserCmd, controlCommand.maybeObsId).add(disperserKey.set(target))
          commandResponseManager.addSubCommand(s.runId, setup.runId)
          val demandMatcher =
            DemandMatcherAll(DemandState(disperserPrefix, disperserStateName).add(disperserKey.set(target)), timeout)
          val response = disperserHcd.onewayAndMatch(setup, demandMatcher)
          response.onComplete {
            case Success(commandResponse) =>
              log.info(s"Set disperser reponded with $commandResponse")
              commandResponseManager.updateSubCommand(setup.runId, commandResponse)
            case Failure(ex) =>
              log.error(s"Set disperser error", ex = ex)
              commandResponseManager.updateSubCommand(setup.runId, Error(setup.runId, ex.toString))
          }
        }

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
