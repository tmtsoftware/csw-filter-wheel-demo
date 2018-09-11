package org.tmt.test.demohcd

import csw.messages.commands.CommandIssue.{MissingKeyIssue, OtherIssue, UnsupportedCommandIssue}
import csw.messages.commands.CommandResponse.Error
import csw.messages.commands.{CommandName, CommandResponse, ControlCommand, Setup}
import csw.messages.params.generics.{Key, KeyType}

import scala.async.Async.async
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.events.{EventKey, EventName}
import csw.messages.params.models.Prefix
import csw.messages.params.states.StateName
import csw.services.location.api.models.{ComponentId, ComponentType, TrackingEvent}
import csw.services.location.api.models.Connection.AkkaConnection

class FilterHcdBehaviorFactory extends ComponentBehaviorFactory {

  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      cswServices: CswServices
  ): ComponentHandlers =
    new FilterHcdHandlers(ctx, cswServices)

}

object FilterHcd {

  val filters = List("None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308")

  val filterKey: Key[String] = KeyType.StringKey.make("filter")

  val filterCmd = CommandName("setFilter")

  // For callers: Must match config file
  val filterPrefix = Prefix("test.FilterHcd")

  val filterConnection = AkkaConnection(ComponentId("FilterHcd", ComponentType.HCD))

  val filterStateName = StateName("FilterState")

  val filterEventName = EventName("filterEvent")

  val filterEventKey = EventKey(filterPrefix, filterEventName)
}

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to DemoHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw-prod/framework.html
 */
class FilterHcdHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    cswServices: CswServices
) extends ComponentHandlers(ctx, cswServices) {
  import FilterHcd._
  import cswServices._

  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger
  private val eventPublisher                = eventService.defaultPublisher

  // Spawn an actor to simulate the work of moving the filter wheel
  private val workActor = ctx.spawn(
    WorkerActor.working(
      1.second,
      currentStatePublisher,
      eventPublisher,
      componentInfo.prefix,
      filterStateName,
      filterKey,
      filters,
      filters.head,
      filters.head,
      filterEventKey
    ),
    "filterWorker"
  )

  override def initialize(): Future[Unit] = async {
    log.debug("Initialize called")
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"onLocationTrackingEvent called: $trackingEvent")
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    controlCommand match {
      case s @ Setup(_, _, `filterCmd`, _, _) =>
        if (s.exists(filterKey)) {
          val filter = s.get(filterKey).get.head
          if (filters.contains(filter))
            CommandResponse.Accepted(controlCommand.runId)
          else
            CommandResponse.Invalid(controlCommand.runId, OtherIssue(s"Unknown filter: $filter"))
        } else {
          CommandResponse.Invalid(controlCommand.runId, MissingKeyIssue(s"Missing ${filterKey.keyName} key"))
        }

      case x =>
        CommandResponse.Invalid(controlCommand.runId, UnsupportedCommandIssue(s"Unsupported command: $x"))
    }
  }

  override def onSubmit(controlCommand: ControlCommand): Unit = {
    log.debug(s"onSubmit called: $controlCommand")
    commandResponseManager.addOrUpdateCommand(controlCommand.runId,
                                              Error(controlCommand.runId, s"Unsupported submit command: $controlCommand"))
  }

  override def onOneway(controlCommand: ControlCommand): Unit = {
    log.debug("onOneway called")

    controlCommand match {
      case s @ Setup(_, _, `filterCmd`, _, _) =>
        val targetFilter = s.get(filterKey).get.head
        workActor ! targetFilter

      case x =>
        log.error(s"Unsupported command received: $x")
    }
  }

  override def onShutdown(): Future[Unit] = async {
    log.debug("onShutdown called")
  }

  override def onGoOffline(): Unit = log.debug("onGoOffline called")

  override def onGoOnline(): Unit = log.debug("onGoOnline called")

}
