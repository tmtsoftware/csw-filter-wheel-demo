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

class DisperserHcdBehaviorFactory extends ComponentBehaviorFactory {

  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      cswServices: CswContext
  ): ComponentHandlers =
    new DisperserHcdHandlers(ctx, cswServices)

}

object DisperserHcd {

  val disperserKey: Key[Int] = KeyType.IntKey.make("disperser")

  val disperserCmd = CommandName("setDisperser")

  // For callers: Must match config file
  val disperserPrefix = Prefix("test.DisperserHcd")

  val disperserConnection = AkkaConnection(ComponentId("DisperserHcd", ComponentType.HCD))

  val disperserStateName = StateName("DisperserState")

  // XXX TODO: Get this from the config service
  val numDispersers = 8
}

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to DemoHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw-prod/framework.html
 */
class DisperserHcdHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    cswCtx: CswContext
) extends ComponentHandlers(ctx, cswCtx) {
  import DisperserHcd._
  import cswCtx._

  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger

  // Spawn an actor to simulate the work of moving the disperser wheel
  private val workActor = ctx.spawn(
    WorkerActor.working(
      1.second,
      currentStatePublisher,
      componentInfo.prefix,
      disperserStateName,
      disperserKey,
      numDispersers,
      0,
      0
    ),
    "disperserWorker"
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
      case s @ Setup(_, _, `disperserCmd`, _, _) =>
        if (s.exists(disperserKey)) {
          val disperser = s.get(disperserKey).get.head
          if (disperser >= 0 && disperser < numDispersers)
            CommandResponse.Accepted(controlCommand.runId)
          else
            CommandResponse.Invalid(controlCommand.runId, OtherIssue(s"Unknown disperser index: $disperser"))
        } else {
          CommandResponse.Invalid(controlCommand.runId, MissingKeyIssue(s"Missing ${disperserKey.keyName} key"))
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
      case s @ Setup(_, _, `disperserCmd`, _, _) =>
        val targetDisperser = s.get(disperserKey).get.head
        workActor ! targetDisperser

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
