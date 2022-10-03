
name := "gepriscrawler"

version := "0.4"

scalaVersion := "2.12.17"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.2.10",
  "com.typesafe.akka" %% "akka-actor" % "2.6.20",
  "com.typesafe.akka" %% "akka-stream" % "2.6.20",
  "org.jsoup" % "jsoup" % "1.15.3",
  "ch.qos.logback" % "logback-classic" % "1.4.1",
  "com.github.tototoshi" %% "scala-csv" % "1.3.10",
  "org.specs2" %% "specs2-core" % "4.2.0",
  "com.typesafe" % "config" % "1.4.2",
  "commons-io" % "commons-io" % "2.11.0",
  "org.zeroturnaround" % "zt-zip" % "1.15",

  "org.xerial" % "sqlite-jdbc" % "3.39.3.0",
  "org.scalikejdbc" %% "scalikejdbc" % "3.1.+",
  "org.scalikejdbc" %% "scalikejdbc-streams" % "3.1.0",
  "com.github.scopt" %% "scopt" % "3.7.0"

)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
mainClass in Compile := Some("gepriscrawler.App")

scalacOptions in Test ++= Seq("-Yrangepos")

