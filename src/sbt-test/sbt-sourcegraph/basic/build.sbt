import scala.collection.JavaConverters._
import java.nio.file.Paths
import java.nio.file.Files

inThisBuild(
  List(
    scalaVersion := "2.13.8",
    organization := "com.example"
  )
)

lazy val a = project
  .settings(
    libraryDependencies += "com.lihaoyi" %% "geny" % "0.6.10",
    libraryDependencies += "junit" % "junit" % "4.13.2"
  )

lazy val b = project
  .dependsOn(a)

commands += Command.command("checkLsif") { s =>
  val dumpPath =
    (ThisBuild / baseDirectory).value / "target" / "sbt-sourcegraph" / "dump.lsif"
  val dump = Files.readAllLines(dumpPath.toPath).asScala
  val packageInformation =
    """.*"name":"(.*)","manager":"jvm-dependencies"}""".r
  val jvmDependencies = dump
    .collect { case packageInformation(name) =>
      name
    }
    .distinct
    .sorted
  if (
    jvmDependencies != List(
      "jdk",
      "maven/com.lihaoyi/geny_2.12",
      "maven/junit/junit",
      "maven/org.scala-lang/scala-library"
    )
  ) {
    sys.error(jvmDependencies.toString)
  }
  s
}
