package com.sourcegraph.sbtsourcegraph

import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
import scala.collection.JavaConverters._
import scala.sys.process._
import java.io.File
import java.nio.file.Path

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
  private var printJavaVersionPath = Option.empty[Path]
  private def printJavaVersionFolder = {
    def create = {
      val dir =
        Files.createTempDirectory("print-java-version")
      val file = dir.resolve("PrintJavaVersion.class")
      val base64 = scala.io.Source
        .fromInputStream(
          getClass()
            .getResourceAsStream(
              "/sbt-sourcegraph/PrintJavaVersion.class.base64"
            )
        )
        .mkString
        .trim

      val contents = java.util.Base64.getDecoder().decode(base64)

      Files.write(file, contents)
      dir
    }

    printJavaVersionPath.synchronized {
      printJavaVersionPath match {

        case Some(value)
            if value.resolve("PrintJavaVersion.class").toFile.isFile =>
          value
        case _ =>
          val created = create
          printJavaVersionPath = Some(created)
          created

      }
    }

  }

  def isJavaAtLeast(n: Int, home: Option[File] = None): Boolean = {

    val significant = jvmVersionCache.getOrElseUpdate(
      home, {
        val raw =
          home match {
            case None =>
              System.getProperty("java.version")
            case Some(javaHome) =>
              val dir = printJavaVersionFolder

              val cmd =
                if (scala.util.Properties.isWin)
                  Paths.get("bin", "java")
                else Paths.get("bin", "java")

              scala.sys.process
                .Process(
                  Seq(
                    cmd.toString(),
                    "-cp",
                    dir.toString(),
                    "PrintJavaVersion"
                  ),
                  cwd = javaHome
                )
                .!!
          }

        val prop = raw.takeWhile(c => c.isDigit || c == '.')

        val segments = prop.split("\\.").toList

        segments match {
          // Java 1.6 - 1.8
          case "1" :: lessThan8 :: _ :: Nil => lessThan8.toInt
          // Java 17.0.1, 11.0.20.1, ..
          case modern :: _ :: _ :: rest => modern.toInt
          // Java 12
          case modern :: Nil => modern.toInt
          case other =>
            sys.error(
              s"Cannot process [java.version] property, unknown format: [$raw]"
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
