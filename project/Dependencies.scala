import sbt._

object Dependencies {

  val `demo-assembly-deps` = Seq(
    CSW.`csw-framework`,
    Libs.`scalatest` % Test,
    Libs.`junit` % Test,
    Libs.`junit-interface` % Test
  )

  val `demo-hcd-deps` = Seq(
    CSW.`csw-framework`,
    Libs.`scalatest` % Test,
    Libs.`junit` % Test,
    Libs.`junit-interface` % Test
  )

  val `demo-deploy-deps` = Seq(
    CSW.`csw-framework`
  )
}
