package org.tmt.test.demodeploy

import java.net.InetAddress

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.actor.typed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, MutableBehavior}
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.logging.scaladsl.{GenericLoggerFactory, LoggingSystemFactory}
import akka.actor.typed.scaladsl.adapter._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.Timeout
import csw.messages.commands.CommandResultType.Negative
import csw.messages.commands.{CommandResponse, Setup}
import csw.messages.events._
import csw.messages.location.ComponentType.Assembly
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location._
import csw.messages.params.models.ObsId
import csw.services.command.scaladsl.CommandService
import csw.services.event.EventServiceFactory
import csw.services.event.api.scaladsl.EventService
import csw.services.location.commons.ClusterAwareSettings

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

// A client to test locating and communicating with the demo assembly and HCDs
object DemoAssemblyTestClient extends App {
  // The following imports are just for convenience, to avoid hard-coding the key names, etc.
  import org.tmt.test.demoassembly.DemoAssembly._
  import org.tmt.test.demohcd.FilterHcd._
  import org.tmt.test.demohcd.DisperserHcd._

  implicit val system: ActorSystem = ClusterAwareSettings.system

  import system.dispatcher

  private val locationService = LocationServiceFactory.withSystem(system)
  private val host            = InetAddress.getLocalHost.getHostName
  LoggingSystemFactory.start("TestServiceClientApp", "0.1", host, system)
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout       = Timeout(3.seconds)
  private val log                     = GenericLoggerFactory.getLogger
  log.info("Starting DemoAssemblyTestClient")
  private val obsId = ObsId("2023-Q22-4-33")

  lazy val eventService: EventService =
    new EventServiceFactory().make(locationService)

  // Actor to receive HCD events
  object EventHandler {
    def make(): Behavior[Event] = {
      log.info("Starting event handler")
      Behaviors.setup(ctx ⇒ new EventHandler(ctx))
    }
  }

  class EventHandler(ctx: ActorContext[Event]) extends MutableBehavior[Event] {
    override def onMessage(msg: Event): Behavior[Event] = {
      msg match {
        case e: SystemEvent =>
          e.get(filterKey)
            .foreach { p =>
              val eventValue = p.head
              log.info(s"Received filter event with value: $eventValue")
            }
          e.get(disperserKey)
            .foreach { p =>
              val eventValue = p.head
              log.info(s"Received disperser event with value: $eventValue")
            }
          Behaviors.same
        case _ => throw new RuntimeException("Expected SystemEvent")
      }
    }
  }

  def startSubscribingToEvents(ctx: ActorContext[TrackingEvent]) = {
    val subscriber   = eventService.defaultSubscriber
    val eventHandler = ctx.spawnAnonymous(EventHandler.make())
    subscriber.subscribeActorRef(Set(filterEventKey, disperserEventKey), eventHandler)
  }

  system.spawn(initialBehavior, "DemoAssemblyTestClient")

  def initialBehavior: Behavior[TrackingEvent] =
    Behaviors.setup { ctx =>
      val connection = AkkaConnection(ComponentId("DemoAssembly", Assembly))
      locationService.subscribe(connection, { loc =>
        ctx.self ! loc
      })
      startSubscribingToEvents(ctx)
      subscriberBehavior
    }

  def subscriberBehavior: Behavior[TrackingEvent] = {
    Behaviors.receive[TrackingEvent] { (ctx, msg) =>
      msg match {
        case LocationUpdated(loc) =>
          log.info(s"LocationUpdated: $loc")
          implicit val sys: typed.ActorSystem[Nothing] = ctx.system
          interact(ctx, new CommandService(loc.asInstanceOf[AkkaLocation]))
        case LocationRemoved(loc) =>
          log.info(s"LocationRemoved: $loc")
      }
      Behaviors.same
    } receiveSignal {
      case (ctx, x) =>
        log.info(s"${ctx.self} received signal $x")
        Behaviors.stopped
    }
  }

  private def makeSetup(filter: String, disperser: String): Setup = {
    val i1 = filterKey.set(filter)
    val i2 = disperserKey.set(disperser)
    Setup(demoPrefix, demoCmd, Some(obsId)).add(i1).add(i2)
  }

  private def interact(ctx: ActorContext[TrackingEvent], assembly: CommandService): Unit = {
    val n      = math.min(filters.size, dispersers.size)
    val setups = (0 until n).toList.map(i => makeSetup(filters(i), dispersers(i)))
    submitAll(setups, assembly).onComplete {
      case Success(responses) => println(s"Test Passed: Responses = $responses")
      case Failure(ex)        => println(s"Test Failed: $ex")
    }
  }

  /**
   * Submits the given setups, one after the other, and returns a future list of command responses.
   * @param setups the setups to submit
   * @param assembly the assembly to submit the setups to
   * @return future list of responses
   */
  private def submitAll(setups: List[Setup], assembly: CommandService): Future[List[CommandResponse]] = {
    Source(setups)
      .mapAsync(1)(assembly.submitAndSubscribe)
      .map { response =>
        if (response.resultType == Negative)
          throw new RuntimeException(s"Command failed: $response")
        else
          println(s"Command response: $response")
        response
      }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(_.toList)
  }
}
