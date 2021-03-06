addSbtPlugin("org.scalastyle"   %%  "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("com.geirsson"     %   "sbt-scalafmt"          % "1.4.0")
addSbtPlugin("org.scoverage"    %   "sbt-scoverage"         % "1.5.1")
addSbtPlugin("com.typesafe.sbt" %   "sbt-native-packager"   % "1.3.3")
addSbtPlugin("com.eed3si9n"     %   "sbt-buildinfo"         % "0.8.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.5.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.4.0")

// web client
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.25")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.13.1")

classpathTypes += "maven-plugin"


