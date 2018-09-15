import sbt.Keys.{libraryDependencies, resolvers}
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.npmDependencies

lazy val `demo-assembly` = project
  .settings(
    libraryDependencies ++= Dependencies.`demo-assembly-deps`
  )
  .dependsOn(`demo-hcd`)

lazy val `demo-hcd` = project
  .settings(
    libraryDependencies ++= Dependencies.`demo-hcd-deps`
  )

lazy val `demo-deploy` = project
  .dependsOn(
    `demo-assembly`,
    `demo-hcd`
  )
  .enablePlugins(DeployApp, CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.`demo-deploy-deps`
  )

lazy val `demo-web-app` = project
  .enablePlugins(ScalaJSBundlerPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    npmDependencies in Compile ++= Seq(
      "react"     -> "16.4.1",
      "react-dom" -> "16.4.2"
    ),
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    libraryDependencies ++= Seq(
      React4s.`react4s`.value,
      React4s.`router4s`.value,
      Utils.`play-json`.value,
      Utils.`enumeratum`.value,
      Utils.`enumeratum-play-json`.value,
      ESW.`ocs-api`.value
    ),
    version in webpack := "4.8.1",
    version in startWebpackDevServer := "3.1.4",
    webpackResources := webpackResources.value +++
    PathFinder(Seq(baseDirectory.value / "index.html")) ** "*.*",
    webpackDevServerExtraArgs in fastOptJS ++= Seq(
      "--content-base",
      baseDirectory.value.getAbsolutePath
    )
  )
