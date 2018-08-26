package org.tmt.test.demohcd
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import csw.framework.CurrentStatePublisher
import csw.messages.events.{EventKey, SystemEvent}
import csw.messages.params.generics.Key
import csw.messages.params.models.Prefix
import csw.messages.params.states.{CurrentState, StateName}
import csw.services.event.api.scaladsl.EventPublisher

import scala.concurrent.duration.FiniteDuration

// An actor that simulates slowly moving through a series of values until arriving at the target value.
// The actor publishes CurrentState messages while updating the value.
private[demohcd] object WorkerActor {
  def working(delay: FiniteDuration,
              currentStatePublisher: CurrentStatePublisher,
              eventPublisher: EventPublisher,
              prefix: Prefix,
              currentStateName: StateName,
              key: Key[String],
              values: List[String],
              targetValue: String,
              currentValue: String,
              eventKey: EventKey): Behavior[String] = Behaviors.receive { (ctx, newTargetValue) =>
    currentStatePublisher.publish(CurrentState(prefix, currentStateName, Set(key.set(currentValue))))
    eventPublisher.publish(SystemEvent(eventKey.source, eventKey.eventName).add(key.set(currentValue)))

    if (currentValue == newTargetValue) Behaviors.same
    else {
      val i               = values.indexOf(currentValue) + 1
      val newCurrentValue = values(i % values.size)
      ctx.schedule(delay, ctx.self, newTargetValue)
      working(delay,
              currentStatePublisher,
              eventPublisher,
              prefix,
              currentStateName,
              key,
              values,
              newTargetValue,
              newCurrentValue,
              eventKey)
    }
  }
}
