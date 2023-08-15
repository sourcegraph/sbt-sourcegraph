package com.sourcegraph.sbtsourcegraph

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
import scala.collection.JavaConverters._
import scala.sys.process._

object Versions {
  def scalametaVersion = "4.4.26"
  private def semanticdbJavacKey = "semanticdb-javac"

  def semanticdbJavacVersion(): String =
    semanticdbVersion(semanticdbJavacKey).getOrElse(
      throw new NoSuchElementException(semanticdbJavacKey)
    )
  def semanticdbVersion(scalaVersion: String): Option[String] =
    // Scala 3 has semanticdb generation built in, so we don't need to
    // add the semanticdb-scalac plugin
    if (scalaVersion.startsWith("3."))
      None
    else
      cachedSemanticdbVersionsByScalaVersion.get(scalaVersion)
  lazy val cachedSemanticdbVersionsByScalaVersion: Map[String, String] = {
    val key = "/sbt-sourcegraph/semanticdb.properties"
    val in = this.getClass().getResourceAsStream(key)
    val props = new Properties()
    if (in != null) {
      props.load(in)
      props.asScala.toMap
    } else {
      Map(
        semanticdbJavacKey -> "0.8.0",
        "2.12.12" -> scalametaVersion,
        "2.13.6" -> scalametaVersion,
        "2.11.12" -> scalametaVersion
      ).withDefaultValue(scalametaVersion)
    }
  }

  def semanticdbVersionsByScalaVersion(): Map[String, String] = {
    val tmp = Files.createTempDirectory("sbt-sourcegraph")
    val coursier = SourcegraphPlugin.createCoursierBinary(tmp.toFile)
    val semanticdbJavacVersions = proc(
      coursier,
      "complete",
      "com.sourcegraph:semanticdb-javac:"
    )
    val artifactIds = proc(
      coursier,
      "complete",
      "org.scalameta:semanticdb-scalac_"
    )
    val versions = for {
      artifactId <- artifactIds.par
      Array(_, scalaVersion) <- List(artifactId.split("_", 2))
      version <- proc(
        coursier,
        "complete",
        s"org.scalameta:$artifactId:"
      )
    } yield scalaVersion -> version
    Files.deleteIfExists(Paths.get(coursier))
    versions.toList.toMap
      .updated(semanticdbJavacKey, semanticdbJavacVersions.last)
  }

  private def proc(cmd: String*): List[String] = {
    println(cmd.updated(0, "coursier").mkString("$ ", " ", ""))
    cmd.!!.linesIterator.toList
  }
}
