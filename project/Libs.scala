import sbt._
import Def.{setting => dep}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Libs {
  val ScalaVersion = "2.13.0"

  val `scala-async` = "org.scala-lang.modules" %% "scala-async" % "0.10.0" //BSD 3-clause "New" or "Revised" License
}

object CSW {
  private val Org     = "com.github.tmtsoftware.csw"
//  private val Version = "0.1-SNAPSHOT"
  private val Version = "v1.0.0-RC4"

  val `csw-config-client` = Org %% "csw-config-client" % Version
  val `csw-framework`     = Org %% "csw-framework" % Version
  val `csw-params`        = dep(Org %%% "csw-params" % Version)
}

object ESW {
  private val Org     = "com.github.tmtsoftware.esw"
  private val Version = "0.1-SNAPSHOT"

  val `esw-ocs-api` = dep(Org %%% "esw-ocs-api" % Version)
}

object Galil {
  private val Org     = "com.github.tmtsoftware.galil-prototype"
  private val Version = "0.1-SNAPSHOT"

  val `galil-io` = Org %% "galil-io" % Version
}

object React4s {
  val `react4s`       = dep("com.github.ahnfelt" %%% "react4s"       % "0.9.27-SNAPSHOT")
  val `jquery-facade` = dep("org.querki"         %%% "jquery-facade" % "1.2")
}

object Utils {
  val `play-json`            = dep("com.typesafe.play" %%% "play-json"            % "2.7.4") //Apache 2.0
  val `enumeratum`           = dep("com.beachape"      %%% "enumeratum"           % "1.5.13")
  val `enumeratum-play-json` = dep("com.beachape"      %%% "enumeratum-play-json" % "1.5.16")
}
