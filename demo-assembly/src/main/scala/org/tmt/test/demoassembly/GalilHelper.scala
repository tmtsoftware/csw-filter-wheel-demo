package org.tmt.test.demoassembly
import akka.util.Timeout
import csw.command.api.StateMatcher
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandResponseManager
import csw.logging.scaladsl.Logger
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.Prefix
import csw.params.core.states.{CurrentState, StateName}
import csw.proto.galil.io.DataRecord

import scala.async.Async.{async, await}
import scala.concurrent.{Await, ExecutionContext}

object GalilHelper {
  // TODO: Get these values from a galil-prototype dependency?
  val axisKey: Key[Char]     = KeyType.CharKey.make("axis")
  val countsKey: Key[Int]    = KeyType.IntKey.make("counts")
  val smoothKey: Key[Double] = KeyType.DoubleKey.make("smooth")
  val lcParamKey: Key[Int]   = KeyType.IntKey.make("lcParam")
  val speedKey: Key[Int]     = KeyType.IntKey.make("speed")

  // Initialize the Galil device
  def init(hcd: CommandService, prefix: Prefix, axis: Char)(implicit timeout: Timeout): CommandResponse = {
    val commands = List(
      // MO
      Setup(prefix, CommandName("motorOff"), None).add(axisKey.set(axis)),
      // DP
      Setup(prefix, CommandName("setMotorPosition"), None).add(axisKey.set(axis)).add(countsKey.set(0)),
      // PT
      Setup(prefix, CommandName("setPositionTracking"), None).add(axisKey.set(axis)).add(countsKey.set(0)),
      // MT - Motor Type (stepper)
      Setup(prefix, CommandName("setMotorType"), None).add(axisKey.set(axis)).add(countsKey.set(2)),
      // AG - Amplifier Gain: Maximum current 1.4A
      Setup(prefix, CommandName("setAmplifierGain"), None).add(axisKey.set(axis)).add(countsKey.set(2)),
      // YB
      Setup(prefix, CommandName("setStepMotorResolution"), None).add(axisKey.set(axis)).add(countsKey.set(200)),
      // KS
      Setup(prefix, CommandName("setMotorSmoothing"), None).add(axisKey.set(axis)).add(smoothKey.set(8)),
      // AC
      Setup(prefix, CommandName("setAcceleration"), None).add(axisKey.set(axis)).add(countsKey.set(1024)),
      // DC
      Setup(prefix, CommandName("setDeceleration"), None).add(axisKey.set(axis)).add(countsKey.set(1024)),
      // LC - Low current mode.  setting is a guess.
      Setup(prefix, CommandName("setLowCurrent"), None).add(axisKey.set(axis)).add(lcParamKey.set(2)),
      // SH
      Setup(prefix, CommandName("motorOn"), None).add(axisKey.set(axis)),
      // SP - set speed in steps per second
      Setup(prefix, CommandName("setMotorSpeed"), None).add(axisKey.set(axis)).add(speedKey.set(25)),
    )

    val responses = Await.result(hcd.submitAll(commands), timeout.duration)
    returnResponse(responses)
  }

  // Set the motor's position, where targetPos is the index of the filter or disperer
  def setPosition(hcd: CommandService,
                  log: Logger,
                  prefix: Prefix,
                  axis: Char,
                  targetPos: Int,
                  commandResponseManager: CommandResponseManager,
                  parentCmd: Setup)(implicit ec: ExecutionContext, timeout: Timeout): Unit = async {
    // Assumes 200 is full rotation and there are 8 filters...
    val pos = (targetPos * 25) % 200
    val setAbsTarget =
      Setup(prefix, CommandName("setAbsTarget"), parentCmd.maybeObsId).add(axisKey.set(axis)).add(countsKey.set(pos))
    commandResponseManager.addSubCommand(parentCmd.runId, setAbsTarget.runId)
    val resp1 = await(hcd.submit(setAbsTarget))
    resp1 match {
      case Completed(_) =>
        val beginMotion = Setup(prefix, CommandName("beginMotion"), parentCmd.maybeObsId).add(axisKey.set(axis))
        val matcher     = new GalilStateMatcher(axis, pos)
        commandResponseManager.addSubCommand(parentCmd.runId, beginMotion.runId)
        commandResponseManager.updateSubCommand(resp1)
        val resp = await(hcd.onewayAndMatch(beginMotion, matcher))
        commandResponseManager.updateSubCommand(resp.asInstanceOf[SubmitResponse])

      case _ =>
        commandResponseManager.updateSubCommand(resp1)
    }
  }

  // Returns the first error or if there are no errors the first response
  private def returnResponse(responses: List[CommandResponse]): CommandResponse = {
    responses
      .find {
        case Completed(_) | CompletedWithResult(_, _) => false
        case _                                        => true
      }
      .getOrElse(responses.head)
  }

  class GalilStateMatcher(axis: Char, pos: Int)(implicit val timeout: Timeout) extends StateMatcher {
    override def prefix: Prefix       = Prefix("galil.hcd")
    override def stateName: StateName = StateName("DataRecord")
    override def check(cs: CurrentState): Boolean = {
      val dataRecord = DataRecord(Result(Prefix(prefix.prefix), cs.paramSet))
      val index      = axis - 'A'
      val currentPos = dataRecord.axisStatuses(index).referencePosition
      println(s"XXX axis $axis: want $pos, have $currentPos")
      currentPos == pos
    }
  }

}
