/**
 * Adaptation of the `scalafixEnable` command from sbt-scalafix with the following modifications:
 * - use older SemanticDB versions instead of upgrading the Scala version.
 * - configure the semanticdb-javac compiler plugin for Java projects.
 * Original license: Apache 2
 * Original source: https://github.com/scalacenter/sbt-scalafix/blob/cdee753f15bde75d84d93c26695d14fc3ec964f8/src/main/scala/scalafix/sbt/ScalafixEnable.scala
 */
package com.sourcegraph.sbtsourcegraph

import sbt._
import sbt.Keys._
import sbt.internal.sbtsourcegraph.Compat

object SourcegraphEnable {

  lazy val command: Command =
    if (SemanticdbPlugin.isAvailable()) withSemanticdbPlugin
    else withSemanticdbScalac

  private lazy val withSemanticdbPlugin = Command.command(
    "sourcegraphEnable",
    briefHelp = "Configure SemanticdbPlugin for Sourcegraph.",
    detail =
      """1. set semanticdbEnabled where supported
        |2. conditionally sets semanticdbVersion & scalaVersion when support is not built-in in the compiler""".stripMargin
  ) { s =>
    val extracted = Project.extract(s)
    val scalacOptionsSettings = Seq(Compile, Test).flatMap(
      inConfig(_)(SourcegraphPlugin.relaxScalacOptionsConfigSettings)
    )
    val semanticdbJavacVersion = Versions.semanticdbJavacVersion()
    val settings = for {
      (p, semanticdbVersion, overriddenScalaVersion) <- collectProjects(
        extracted
      )
      enableSemanticdbPlugin =
        List(
          Option(
            (p / allDependencies) +=
              "com.sourcegraph" % "semanticdb-javac" % semanticdbJavacVersion
          ),
          Option(
            (p / javacOptions) += s"-Xplugin:semanticdb " +
              s"-build-tool:sbt " +
              s"-sourceroot:${(ThisBuild / baseDirectory).value} " +
              s"-targetroot:${(p / Compile / classDirectory).value.toPath().resolveSibling("semanticdb-classes")}"
          ),
          overriddenScalaVersion.map(v => (p / scalaVersion) := v),
          Option((p / SemanticdbPlugin.semanticdbEnabled) := true),
          Option((p / SemanticdbPlugin.semanticdbVersion) := semanticdbVersion)
        ).flatten
      settings <-
        inScope(ThisScope.in(p))(
          scalacOptionsSettings
        ) ++ enableSemanticdbPlugin
    } yield settings
    Compat.append(extracted, settings, s)
  }

  private lazy val withSemanticdbScalac = Command.command(
    "sourcegraphEnable",
    briefHelp =
      "Configure libraryDependencies, scalaVersion and scalacOptions for sourcegraph.",
    detail =
      """1. enables the semanticdb-scalac compiler plugin
        |2. sets scalaVersion to latest Scala version supported by sourcegraph
        |3. add -Yrangepos to Compile|Test / compile / scalacOptions""".stripMargin
  ) { s =>
    val extracted = Project.extract(s)
    val scalacOptionsSettings = Seq(Compile, Test).flatMap(
      inConfig(_)(
        semanticdbConfigSettings ++
          SourcegraphPlugin.relaxScalacOptionsConfigSettings
      )
    )
    val settings: Seq[Setting[_]] = for {
      (p, semanticdbVersion, overriddenScalaVersion) <- collectProjects(
        extracted
      )
      isSemanticdbEnabled =
        (p / libraryDependencies)
          .get(extracted.structure.data)
          .exists(_.exists(_.name == "semanticdb-scalac"))
      if !isSemanticdbEnabled
      addSemanticdbCompilerPlugin = List(
        overriddenScalaVersion.map { v =>
          (p / scalaVersion) := v
        },
        Option(
          (p / allDependencies) += compilerPlugin(
            SourcegraphPlugin.autoImport.sourcegraphSemanticdb(
              semanticdbVersion
            )
          )
        )
      ).flatten
      settings <-
        inScope(ThisScope.in(p))(
          scalacOptionsSettings
        ) ++ addSemanticdbCompilerPlugin
    } yield settings
    Compat.append(extracted, settings, s)
  }

  private val semanticdbConfigSettings: Seq[Def.Setting[_]] =
    Seq(
      (compile / scalacOptions) := {
        val old = (compile / scalacOptions).value
        val options = List(
          "-Yrangepos",
          "-Xplugin-require:semanticdb"
        )
        // don't rely on autoCompilerPlugins to inject the plugin as it does not work if scalacOptions is overriden in the build
        val plugins = Compat.autoPlugins(update.value, scalaVersion.value)
        old ++ (plugins ++ options).diff(old)
      }
    )

  private def collectProjects[U](
      extracted: Extracted
  ): Seq[(ProjectRef, String, Option[String])] = for {
    p <- extracted.structure.allProjectRefs
    projectScalaVersion <- (p / scalaVersion)
      .get(extracted.structure.data)
      .toList
    overriddenScalaVersion =
      if (
        projectScalaVersion.startsWith("2.11") &&
        Versions.semanticdbVersion(projectScalaVersion).isEmpty
      )
        Some("2.11.12")
      else None
    semanticdbVersion <- Versions
      .semanticdbVersion(overriddenScalaVersion.getOrElse(projectScalaVersion))
      .toList
  } yield (p, semanticdbVersion, overriddenScalaVersion)
}
