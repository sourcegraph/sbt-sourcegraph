package com.sourcegraph.sbtsourcegraph

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
import scala.collection.JavaConverters._
import scala.sys.process._
import java.io.File

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

  private val jvmVersionCache = collection.mutable.Map.empty[Option[File], Int]

  def isJavaAtLeast(n: Int, home: Option[File] = None) = {

    val significant = jvmVersionCache.getOrElseUpdate(
      home, {
        val raw =
          home match {
            case None =>
              System.getProperty("java.version")
            case Some(javaHome) =>
              val sb = new StringBuilder
              val proc = {
                val cmd =
                  if (scala.util.Properties.isWin)
                    Paths.get("bin", "javac.exe")
                  else Paths.get("bin", "javac")

                val stdout = scala.sys.process
                  .Process(Seq(cmd.toString(), "-version"), cwd = javaHome)
                  .!!(ProcessLogger(sb.append(_)))
                  .trim

                // Java 8 sends output to stderr...
                if (stdout.isEmpty()) sb.result().trim else stdout
              }

              proc.split(" ").toList match {
                case "javac" :: version :: Nil => version
                case other =>
                  sys.error(
                    s"Cannot process javac output (in $javaHome): [$proc]"
                  )
              }
          }

        val prop = raw.takeWhile(c => c.isDigit || c == '.')

        val segments = prop.split("\\.").toList

        segments match {
          // Java 1.6 - 1.8
          case "1" :: lessThan8 :: _ :: Nil => lessThan8.toInt
          // Java 17.0.1, ..
          case modern :: _ :: _ :: Nil => modern.toInt
          // Java 12
          case modern :: Nil => modern.toInt
          case other =>
            sys.error(
              s"Cannot process java.home property, unknown format: [$raw]"
            )
        }
      }
    )

    significant >= n
  }

  private def proc(cmd: String*): List[String] = {
    println(cmd.updated(0, "coursier").mkString("$ ", " ", ""))
    cmd.!!.linesIterator.toList
  }
}
