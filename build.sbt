import scala.jdk.CollectionConverters._
import java.util.Properties
import com.sourcegraph.sbtsourcegraph.Versions

val V = new {
  def scala210 = "2.10.7"
  def scala212 = "2.12.17"
  def scalameta = "4.7.7"
}

scalaVersion := V.scala212
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0-alpha.1"
ThisBuild / versionScheme := Some("early-semver")
organization := "com.sourcegraph"
semanticdbEnabled := !scalaVersion.value.startsWith("2.10")
semanticdbVersion := V.scalameta
homepage := Some(url("https://github.com/sourcegraph/sbt-sourcegraph"))
licenses := List(
  "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
)
developers := List(
  Developer(
    "olafurpg",
    "Ólafur Páll Geirsson",
    "olafurpg@sourcegraph.com",
    url("https://sourcegraph.com")
  )
)

commands +=
  Command.command("fixAll") { s =>
    "scalafixAll" :: "scalafmtAll" :: "scalafmtSbt" :: s
  }

commands +=
  Command.command("checkAll") { s =>
    "scalafmtCheckAll" :: "scalafmtSbtCheck" ::
      "scalafixAll --check" :: "publishLocal" ::
      s
  }

// Cross-building settings (see https://github.com/sbt/sbt/issues/3473#issuecomment-325729747)

sbtPlugin := true
moduleName := "sbt-sourcegraph"
pluginCrossBuild / sbtVersion := "1.2.1"
Compile / resourceGenerators += Def.task {
  val out =
    (Compile / managedResourceDirectories).value.head / "sbt-sourcegraph" / "semanticdb.properties"
  if (!out.exists()) {
    val versions = Versions.semanticdbVersionsByScalaVersion()
    val props = new Properties()
    versions.foreach { case (scalaVersion, semanticdbVersion) =>
      props.put(scalaVersion, semanticdbVersion)
    }
    IO.write(props, "SemanticDB versions grouped by Scala version.", out)
  }
  List(out)
}
crossScalaVersions := Seq(V.scala212, V.scala210)
scalacOptions ++= {
  if (scalaVersion.value == V.scala210) List()
  else List("-Xlint:unused")
}

pluginCrossBuild / sbtVersion := {
  // keep this as low as possible to avoid running into binary incompatibility such as https://github.com/sbt/sbt/issues/5049
  scalaBinaryVersion.value match {
    case "2.10" => "0.13.17"
    case "2.12" => "1.2.1"
  }
}

buildInfoKeys := List[BuildInfoKey](
  version
)
buildInfoPackage := "com.sourcegraph.sbtsourcegraph"
enablePlugins(BuildInfoPlugin)
enablePlugins(ScriptedPlugin)
scriptedBufferLog := false
scriptedLaunchOpts ++= Seq(
  "-Xmx2048M",
  s"-Dscip-java.version=${Versions.semanticdbJavacVersion()}",
  s"-Dplugin.version=${version.value}"
)

def isCI = "true" == System.getenv("CI")
