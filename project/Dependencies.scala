import sbt._

object Dependencies {

  val `demo-assembly-deps` = Seq(
    CSW.`csw-framework`,
    Galil.`galil-io`
  )

  val `demo-hcd-deps` = Seq(
    CSW.`csw-framework`
  )

  val `demo-deploy-deps` = Seq(
    CSW.`csw-framework`
  )
}
