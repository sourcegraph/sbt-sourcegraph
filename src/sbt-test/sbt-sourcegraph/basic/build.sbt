import scala.collection.JavaConverters._
import java.nio.file.Paths
import java.nio.file.Files

inThisBuild(
  List(
    scalaVersion := "2.12.15",
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
    (ThisBuild / baseDirectory).value / "target" / "sbt-sourcegraph" / "index.scip"
  val index =
    lib.codeintel.scip.Scip.Index.parseFrom(Files.readAllBytes(dumpPath.toPath))
  val packageNames = index.getDocumentsList.asScala
    .flatMap(_.getOccurrencesList.asScala)
    .map(_.getSymbol)
    .filterNot(_.startsWith("local"))
    .map(_.split(" ").toList)
    .collect { case _ :: _ :: name :: _ => name }
    .filterNot(_ == ".")
    .distinct
    .sorted
  if (
    packageNames != List(
      "jdk",
      "maven/com.lihaoyi/geny_2.12",
      "maven/junit/junit",
      "maven/org.scala-lang/scala-library"
    )
  ) {
    sys.error(packageNames.toString)
  }
  s
}
