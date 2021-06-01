addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.4")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.29")

Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "plugin" / "src" / "main" / "scala"
Compile / unmanagedResourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile /
    "plugin" / "src" / "main" / "resources"
