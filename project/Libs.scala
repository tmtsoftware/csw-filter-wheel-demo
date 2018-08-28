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
  val Version = "0.1-SNAPSHOT"

  val `csw-location`      = "org.tmt" %% "csw-location"      % Version
  val `csw-config-client` = "org.tmt" %% "csw-config-client" % Version
  val `csw-logging`       = "org.tmt" %% "csw-logging"       % Version
  val `csw-framework`     = "org.tmt" %% "csw-framework"     % Version
  val `csw-command`       = "org.tmt" %% "csw-command"       % Version
  val `csw-messages`      = "org.tmt" %% "csw-messages"      % Version
}

object React4s {
  val `react4s`  = dep("com.github.ahnfelt" %%% "react4s"  % "0.9.15-SNAPSHOT")
  val `router4s` = dep("com.github.werk"    %%% "router4s" % "0.1.0-SNAPSHOT")
  val `jquery-facade` = dep("org.querki" %%% "jquery-facade" % "1.2")
}


