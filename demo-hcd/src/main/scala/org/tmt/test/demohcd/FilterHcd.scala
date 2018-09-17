package org.tmt.test.demohcd

import scala.async.Async.async
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import akka.actor.typed.scaladsl.ActorContext
import csw.command.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.location.api.models.{ComponentId, ComponentType, TrackingEvent}
import csw.location.api.models.Connection.AkkaConnection
import csw.params.commands.CommandIssue.{MissingKeyIssue, OtherIssue, UnsupportedCommandIssue}
import csw.params.commands.CommandResponse.Error
import csw.params.commands.{CommandName, CommandResponse, ControlCommand, Setup}
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.Prefix
import csw.params.core.states.StateName

class FilterHcdBehaviorFactory extends ComponentBehaviorFactory {

  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      cswServices: CswContext
  ): ComponentHandlers =
    new FilterHcdHandlers(ctx, cswServices)

}

object FilterHcd {

  val filterKey: Key[Int] = KeyType.IntKey.make("filter")

  val filterCmd = CommandName("setFilter")

  // For callers: Must match config file
  val filterPrefix = Prefix("test.FilterHcd")

  val filterConnection = AkkaConnection(ComponentId("FilterHcd", ComponentType.HCD))

  val filterStateName = StateName("FilterState")

  // XXX TODO: Get this from the config service
  val numFilters = 8
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
    cswCtx: CswContext
) extends ComponentHandlers(ctx, cswCtx) {
  import FilterHcd._
  import cswCtx._

  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger

  // Spawn an actor to simulate the work of moving the filter wheel
  private val workActor = ctx.spawn(
    WorkerActor.working(
      1.second,
      currentStatePublisher,
      componentInfo.prefix,
      filterStateName,
      filterKey,
      numFilters,
      0,
      0
    ),
    "filterWorker"
  )

  override def initialize(): Future[Unit] = async {
    log.debug("Initialize called")
    workActor ! 0
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"onLocationTrackingEvent called: $trackingEvent")
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    controlCommand match {
      case s @ Setup(_, _, `filterCmd`, _, _) =>
        if (s.exists(filterKey)) {
          val filter = s.get(filterKey).get.head
          if (filter >= 0 && filter < numFilters)
            CommandResponse.Accepted(controlCommand.runId)
          else
            CommandResponse.Invalid(controlCommand.runId, OtherIssue(s"Unknown filter index: $filter"))
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
