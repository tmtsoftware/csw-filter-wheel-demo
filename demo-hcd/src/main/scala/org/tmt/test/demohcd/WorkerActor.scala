package org.tmt.test.demohcd
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import csw.framework.CurrentStatePublisher
import csw.messages.params.generics.Key
import csw.messages.params.models.Prefix
import csw.messages.params.states.{CurrentState, StateName}

import scala.concurrent.duration.FiniteDuration

// An actor that simulates slowly moving through a series of values until arriving at the target value.
// The actor publishes CurrentState messages while updating the value.
private[demohcd] object WorkerActor {
  def working(delay: FiniteDuration,
              currentStatePublisher: CurrentStatePublisher,
              prefix: Prefix,
              currentStateName: StateName,
              key: Key[Int],
              numValues: Int,
              targetValue: Int,
              currentValue: Int): Behavior[Int] = Behaviors.receive { (ctx, newTargetValue) =>
    currentStatePublisher.publish(CurrentState(prefix, currentStateName, Set(key.set(currentValue))))

    if (currentValue == newTargetValue) Behaviors.same
    else {
      val i               = currentValue + 1
      val newCurrentValue = i % numValues
      ctx.schedule(delay, ctx.self, newTargetValue)
      working(delay, currentStatePublisher, prefix, currentStateName, key, numValues, newTargetValue, newCurrentValue)
    }
  }
}
