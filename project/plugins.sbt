addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.29")
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.9-22-2d02726c")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
libraryDependencies ++= List(
  "org.apache.logging.log4j" % "log4j-core" % "2.14.0"
)

Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "src" / "main" / "scala"
Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "src" / "main" / "scala-sbt-1.0"
Compile / unmanagedResourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "src" / "main" / "resources"
