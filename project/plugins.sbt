addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.29")
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.4.8-98-e7d4e01e")

Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "src" / "main" / "scala"
Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "src" / "main" / "scala-sbt-1.0"
Compile / unmanagedResourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "src" / "main" / "resources"
