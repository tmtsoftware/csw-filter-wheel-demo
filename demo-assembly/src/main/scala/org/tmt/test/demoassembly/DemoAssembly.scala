package org.tmt.test.demoassembly

import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.commands.CommandIssue.{MissingKeyIssue, OtherIssue, UnresolvedLocationsIssue, UnsupportedCommandIssue}
import csw.messages.commands.CommandResponse.Error
import csw.messages.commands.matchers.DemandMatcherAll
import csw.messages.commands.{CommandName, CommandResponse, ControlCommand, Setup}
import csw.messages.params.generics.Key
import csw.messages.params.models.Prefix
import csw.messages.params.states.{DemandState, StateName}
import csw.services.command.scaladsl.CommandService
import csw.services.location.api.models.{AkkaLocation, LocationRemoved, LocationUpdated, TrackingEvent}
import org.tmt.test.demohcd.FilterHcd._
import org.tmt.test.demohcd.DisperserHcd._

import scala.concurrent.duration._
import scala.async.Async.async
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class DemoAssemblyBehaviorFactory extends ComponentBehaviorFactory {

  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      cswServices: CswServices
  ): ComponentHandlers =
    new DemoAssemblyHandlers(ctx, cswServices)

}

object DemoAssembly {
  // Name of command sent to this assembly to set filter and disperser values
  val demoCmd = CommandName("demo")

  // For callers: Must match config file
  val demoPrefix = Prefix("test.DemoAssembly")
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
    cswServices: CswServices
) extends ComponentHandlers(ctx, cswServices) {
  import DemoAssembly._
  import cswServices._

  implicit val ec: ExecutionContextExecutor             = ctx.executionContext
  implicit val timeout: Timeout                         = 15.seconds
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
        loc.connection match {
          case `filterConnection` =>
            maybeFilterHcd = Some(new CommandService(loc)(ctx.system))
          case `disperserConnection` =>
            maybeDisperserHcd = Some(new CommandService(loc)(ctx.system))
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
      validateCommand(controlCommand, "filter", maybeFilterHcd, filterKey, filters),
      validateCommand(controlCommand, "disperser", maybeDisperserHcd, disperserKey, dispersers)
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
                       prefix: Prefix,
                       currentStateName: StateName,
                       commandName: CommandName,
                       name: String,
                       maybeHcd: Option[CommandService],
                       key: Key[String]): Unit = {

    maybeHcd.foreach { hcd =>
      controlCommand.get(key).foreach { param =>
        val target = param.head
        val setup  = Setup(componentInfo.prefix, commandName, controlCommand.maybeObsId).add(key.set(target))
        commandResponseManager.addSubCommand(controlCommand.runId, setup.runId)
        val demandMatcher = DemandMatcherAll(DemandState(prefix, currentStateName).add(key.set(target)), timeout)
        val response      = hcd.onewayAndMatch(setup, demandMatcher)
        response.onComplete {
          case Success(commandResponse) =>
            log.info(s"Set $name reponded with $commandResponse")
            commandResponseManager.updateSubCommand(setup.runId, commandResponse)
          case Failure(ex) =>
            log.error(s"Set $name error", ex = ex)
            commandResponseManager.updateSubCommand(setup.runId, Error(setup.runId, ex.toString))
        }
      }
    }
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    log.debug(s"onSubmit called: $controlCommand")

    controlCommand match {
      case s @ Setup(_, _, `demoCmd`, _, _) =>
        onSubmit(s, filterPrefix, filterStateName, filterCmd, "filter", maybeFilterHcd, filterKey)
        onSubmit(s, disperserPrefix, disperserStateName, disperserCmd, "disperser", maybeDisperserHcd, disperserKey)

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
