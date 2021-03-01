inThisBuild(
  List(
    scalaVersion := "2.13.5",
    scalacOptions ++= List(
      "-Xlint:unused"
    ),
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0-alpha.1",
    semanticdbEnabled := true,
    organization := "com.sourcegraph",
    homepage := Some(url("https://github.com/sourcegraph/sbt-sourcegraph")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "olafurpg",
        "Ólafur Páll Geirsson",
        "olafurpg@sourcegraph.com",
        url("https://sourcegraph.com")
      )
    )
  )
)

skip in publish := true

lazy val plugin = project
  .settings(
    sbtPlugin := true,
    moduleName := "sbt-sourcegraph"
  )
