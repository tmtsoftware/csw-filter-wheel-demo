import sbt._
import Def.{setting => dep}
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Libs {
  val ScalaVersion = "2.12.6"

  val `scalatest`       = "org.scalatest"          %% "scalatest"      % "3.0.5"  //Apache License 2.0
  val `scala-async`     = "org.scala-lang.modules" %% "scala-async"    % "0.9.7"  //BSD 3-clause "New" or "Revised" License
  val `junit`           = "junit"                  % "junit"           % "4.12"   //Eclipse Public License 1.0
  val `junit-interface` = "com.novocode"           % "junit-interface" % "0.11"   //BSD 2-clause "Simplified" License
  val `mockito-core`    = "org.mockito"            % "mockito-core"    % "2.21.0" //MIT License
}

object CSW {
//  private val Version = "0.1-SNAPSHOT"
//  private val Org     = "org.tmt"

  private val Org     = "com.github.tmtsoftware.csw-prod"
  private val Version = "6845e21"


  val `csw-location`      = Org %% "csw-location" % Version
  val `csw-config-client` = Org %% "csw-config-client" % Version
  val `csw-logging`       = Org %% "csw-logging" % Version
  val `csw-framework`     = Org %% "csw-framework" % Version
  val `csw-command`       = Org %% "csw-command" % Version
  val `csw-messages`      = Org %% "csw-messages" % Version
  val `csw-params`        = dep(Org %%% "csw-params" % Version)
}

object ESW {
  private val Version = "0.1.0-SNAPSHOT"
  private val Org     = "org.tmt"
  val `sequencer-api` = dep(Org %%% "sequencer-api" % Version)
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
