package org.tmt.test.demohcd

import csw.messages.commands.CommandResponse.Error
import csw.messages.commands.{CommandName, CommandResponse, ControlCommand, Setup}
import csw.messages.params.generics.{Key, KeyType}

import scala.concurrent.duration._
import scala.async.Async.async
import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.TopLevelActorMessage
import csw.messages.commands.CommandIssue.{MissingKeyIssue, OtherIssue, UnsupportedCommandIssue}
import csw.messages.events.{EventKey, EventName}
import csw.messages.params.models.Prefix
import csw.messages.params.states.StateName
import csw.services.location.api.models.{ComponentId, ComponentType, TrackingEvent}
import csw.services.location.api.models.Connection.AkkaConnection

class DisperserHcdBehaviorFactory extends ComponentBehaviorFactory {

  override def handlers(
      ctx: ActorContext[TopLevelActorMessage],
      cswServices: CswServices
  ): ComponentHandlers =
    new DisperserHcdHandlers(ctx, cswServices)
}

object DisperserHcd {

  val dispersers =
    List("Mirror", "B1200_G5301", "R831_G5302", "B600_G5303", "B600_G5307", "R600_G5304", "R400_G5305", "R150_G5306")

  val disperserKey: Key[String] = KeyType.StringKey.make("disperser")

  val disperserCmd = CommandName("setDisperser")

  // For callers: Must match config file
  val disperserPrefix = Prefix("TEST.DisperserHcd")

  val disperserConnection = AkkaConnection(ComponentId("DisperserHcd", ComponentType.HCD))

  val disperserStateName = StateName("DisperserState")

  val disperserEventName = EventName("disperserEvent")

  val disperserEventKey = EventKey(disperserPrefix, disperserEventName)
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
    cswServices: CswServices
) extends ComponentHandlers(ctx, cswServices) {
  import DisperserHcd._
  import cswServices._

  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger
  private val eventPublisher                = eventService.defaultPublisher

  // Spawn an actor to simulate the work of moving the disperser wheel
  private val workActor = ctx.spawn(
    WorkerActor.working(
      1.second,
      currentStatePublisher,
      eventPublisher,
      componentInfo.prefix,
      disperserStateName,
      disperserKey,
      dispersers,
      dispersers.head,
      dispersers.head,
      disperserEventKey,
    ),
    "disperserWorker"
  )

  override def initialize(): Future[Unit] = async {
    log.debug("Initialize called")
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"onLocationTrackingEvent called: $trackingEvent")
  }

  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    controlCommand match {
      case s @ Setup(_, _, `disperserCmd`, _, _) =>
        if (s.exists(disperserKey)) {
          val disperser = s.get(disperserKey).get.head
          if (dispersers.contains(disperser))
            CommandResponse.Accepted(controlCommand.runId)
          else
            CommandResponse.Invalid(controlCommand.runId, OtherIssue(s"Unknown disperser: $disperser"))
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
