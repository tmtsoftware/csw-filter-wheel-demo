package org.tmt.test.demoassembly
import akka.util.Timeout
import csw.command.CommandResponseManager
import csw.command.models.matchers.StateMatcher
import csw.command.scaladsl.CommandService
import csw.params.commands._
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.{ObsId, Prefix}
import csw.params.core.states.CurrentState
import csw.proto.galil.io.DataRecord

import scala.concurrent.Await

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
      Setup(prefix, CommandName("motorOff"), None).add(axisKey.set(axis)),
      Setup(prefix, CommandName("setMotorPosition"), None).add(axisKey.set(axis)).add(countsKey.set(0)),
      Setup(prefix, CommandName("setPositionTracking"), None).add(axisKey.set(axis)).add(countsKey.set(0)),
      Setup(prefix, CommandName("setMotorType"), None).add(axisKey.set(axis)).add(countsKey.set(2)),
      Setup(prefix, CommandName("setAmplifierGain"), None).add(axisKey.set(axis)).add(countsKey.set(2)),
      Setup(prefix, CommandName("setStepDriveResolution"), None).add(axisKey.set(axis)).add(countsKey.set(1)),
      Setup(prefix, CommandName("setStepMotorResolution"), None).add(axisKey.set(axis)).add(countsKey.set(200)),
      Setup(prefix, CommandName("setMotorSmoothing"), None).add(axisKey.set(axis)).add(smoothKey.set(8)),
      Setup(prefix, CommandName("setAcceleration"), None).add(axisKey.set(axis)).add(countsKey.set(1024)),
      Setup(prefix, CommandName("setDeceleration"), None).add(axisKey.set(axis)).add(countsKey.set(1024)),
      Setup(prefix, CommandName("setLowCurrent"), None).add(axisKey.set(axis)).add(lcParamKey.set(2)),
      Setup(prefix, CommandName("motorOn"), None).add(axisKey.set(axis)),
      Setup(prefix, CommandName("setMotorSpeed"), None).add(axisKey.set(axis)).add(speedKey.set(25)),
    )

    val responses = commands.map(cmd => Await.result(hcd.submitAndSubscribe(cmd), timeout.duration))
    returnResponse(responses)
  }

  // Set the motor's position, where targetPos is the index of the filter or disperer
  def setPosition(hcd: CommandService,
                  prefix: Prefix,
                  maybeObsId: Option[ObsId],
                  axis: Char,
                  targetPos: Int,
                  commandResponseManager: CommandResponseManager,
                  parentCmd: Setup)(
      implicit timeout: Timeout
  ): Unit = {
    // Assumes 200 is full rotation and there are 8 filters...
    val pos = (targetPos * 25) % 200
    Await.result(hcd.submit(Setup(prefix, CommandName("setAbsTarget"), None).add(axisKey.set(axis)).add(countsKey.set(pos))),
                 timeout.duration)
    val cmd     = Setup(prefix, CommandName("beginMotion"), maybeObsId).add(axisKey.set(axis))
    val matcher = new GalilStateMatcher(axis, pos)
    commandResponseManager.addSubCommand(parentCmd.runId, cmd.runId)
    val resp = Await.result(hcd.onewayAndMatch(cmd, matcher), timeout.duration)
    commandResponseManager.updateSubCommand(cmd.runId, resp)
  }

  // Returns the first error or if there are no errors the first response
  private def returnResponse(responses: List[CommandResponse]): CommandResponse = {
    responses.find(_.resultType != CommandResultType.Positive).getOrElse(responses.head)
  }

  class GalilStateMatcher(axis: Char, pos: Int)(implicit val timeout: Timeout) extends StateMatcher {
    override def prefix: String    = "galil.hcd"
    override def stateName: String = "DataRecord"
    override def check(cs: CurrentState): Boolean = {
      val dataRecord = DataRecord(Result(Prefix(prefix), cs.paramSet))
      val index      = axis - 'A'
      val currentPos = dataRecord.axisStatuses(index).referencePosition
      println(s"XXX axis $axis: want $pos, have $currentPos")
      currentPos == pos
    }
  }

}
