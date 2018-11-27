import sbt._
import Def.{setting => dep}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Libs {
  val ScalaVersion = "2.12.7"

  val `scalatest`       = "org.scalatest"          %% "scalatest"      % "3.0.5"  //Apache License 2.0
  val `scala-async`     = "org.scala-lang.modules" %% "scala-async"    % "0.9.7"  //BSD 3-clause "New" or "Revised" License
  val `junit`           = "junit"                  % "junit"           % "4.12"   //Eclipse Public License 1.0
  val `junit-interface` = "com.novocode"           % "junit-interface" % "0.11"   //BSD 2-clause "Simplified" License
  val `mockito-core`    = "org.mockito"            % "mockito-core"    % "2.21.0" //MIT License
}

object CSW {
  private val Org     = "com.github.tmtsoftware.csw"
    private val Version = "0.1-SNAPSHOT"
//  private val Version = "ef09b11" // Must match version used by esw-prototype and the version of the installed csw services!

  val `csw-config-client` = Org %% "csw-config-client" % Version
  val `csw-framework`     = Org %% "csw-framework" % Version
  val `csw-params`        = dep(Org %%% "csw-params" % Version)
}

object ESW {
  private val Org = "com.github.tmtsoftware.esw-prototype"
  private val Version = "0.1.0-SNAPSHOT"
//  private val Version = "cb257b8"

  val `ocs-api`       = dep(Org %%% "ocs-api" % Version)
  val `react4s-facade`       = dep(Org %%% "react4s-facade" % Version)
}

object Galil {
    private val Org     = "com.github.tmtsoftware.galil-prototype"
      private val Version = "0.1-SNAPSHOT"
//  private val Version = "f2466a1"

   val `galil-io` = Org %% "galil-io" % Version
}

object React4s {
  val `react4s`       = dep("com.github.ahnfelt" %%% "react4s"       % "0.9.15-SNAPSHOT")
  val `router4s`      = dep("com.github.werk"    %%% "router4s"      % "0.1.0-SNAPSHOT")
  val `jquery-facade` = dep("org.querki"         %%% "jquery-facade" % "1.2")
}

object Utils {
  val `play-json`            = dep("com.typesafe.play" %%% "play-json"            % "2.6.10") //Apache 2.0
  val `enumeratum`           = dep("com.beachape"      %%% "enumeratum"           % "1.5.13")
  val `enumeratum-play-json` = dep("com.beachape"      %%% "enumeratum-play-json" % "1.5.14")
}
