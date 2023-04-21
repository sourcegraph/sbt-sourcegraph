package com.sourcegraph.sbtsourcegraph

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
import scala.collection.JavaConverters._
import scala.sys.process._

object Versions {
  def scalametaVersion = "4.7.7"
  def scala211 = "2.11.12"
  def scala212 = "2.12.17"
  def scala213 = "2.13.10"
  private def semanticdbJavacKey = "semanticdb-javac"

  def semanticdbJavacVersion(): String =
    semanticdbVersion(semanticdbJavacKey).getOrElse(
      throw new NoSuchElementException(semanticdbJavacKey)
    )
  def semanticdbVersion(scalaVersion: String): Option[String] =
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
        scala211 -> scalametaVersion,
        scala212 -> scalametaVersion,
        scala213 -> scalametaVersion
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
