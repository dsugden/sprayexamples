version       := "0.1"

scalaVersion  := "2.11.2"

val Akka = "2.3.5"
val Spray = "1.3.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "spray repo"         at "http://repo.spray.io/",
  "sonatype releases"  at "http://oss.sonatype.org/content/repositories/releases/",
  "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
  "typesafe repo"      at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka"          %% "akka-actor"         % Akka     % "compile",
  "io.spray"                   %% "spray-routing"      % Spray    % "compile",
  "io.spray"                   %% "spray-can"          % Spray    % "compile",
  "io.spray"                   %% "spray-client"       % Spray    % "compile",
  "org.json4s"                 %% "json4s-native"      % "3.2.10" % "compile",
  "org.scalatest"              %% "scalatest"          % "2.2.0"  % "compile",
  "com.typesafe.scala-logging" %% "scala-logging"      % "3.0.0"  % "compile",
  "io.spray"                   %% "spray-testkit"      % Spray    % "test"
)

seq(Revolver.settings: _*)
