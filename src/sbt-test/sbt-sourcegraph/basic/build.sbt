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
    libraryDependencies += "com.lihaoyi" %% "geny" % "0.7.1",
    libraryDependencies += "junit" % "junit" % "4.13.2"
  )

lazy val b = project
  .dependsOn(a)
  .settings(
    // Test to ensure the plugin works with explicitly set java home
    // On Java 8 the java.home property returns JRE path, not JDK path.
    // so we try and work around it hoping that JAVA_HOME is set by executing
    // environment
    javaHome := {
      println(sys.env.get("JAVA_HOME"))
      Some(
        new File(
          sys.env.getOrElse("JAVA_HOME", System.getProperty("java.home"))
        )
      )
    }
  )

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
    .toList
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
