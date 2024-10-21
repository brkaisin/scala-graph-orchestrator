ThisBuild / scalaVersion := "3.5.1"
ThisBuild / organization := "be.brkaisin"
ThisBuild / version := "0.0.1"
ThisBuild / organizationName := "Brieuc Kaisin"

lazy val root = (project in file("."))
  .settings(
    name := "Scala Graph Orchestrator",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % "1.1.2",
      "ch.qos.logback" % "logback-classic" % "1.5.11",
      "dev.zio" %% "zio" % "2.1.11"
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings",
      "-Wvalue-discard"
    ),
    Test / parallelExecution := false,
    fork / run := true
  )
