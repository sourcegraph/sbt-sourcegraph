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
            allDependencies.in(p) +=
              "com.sourcegraph" % "semanticdb-javac" % semanticdbJavacVersion
          ),
          Option(
            javacOptions.in(p) += s"-Xplugin:semanticdb " +
              s"-build-tool:sbt " +
              s"-sourceroot:${baseDirectory.in(ThisBuild).value} " +
              s"-targetroot:${classDirectory.in(p, Compile).value.toPath().resolveSibling("semanticdb-classes")}"
          ),
          overriddenScalaVersion.map(v => scalaVersion.in(p) := v),
          Option(SemanticdbPlugin.semanticdbEnabled.in(p) := true),
          semanticdbVersion.map(ver =>
            SemanticdbPlugin.semanticdbVersion.in(p) := ver
          )
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
        libraryDependencies
          .in(p)
          .get(extracted.structure.data)
          .exists(_.exists(_.name == "semanticdb-scalac"))
      if !isSemanticdbEnabled
      addSemanticdbCompilerPlugin = List(
        overriddenScalaVersion.map { v =>
          scalaVersion.in(p) := v
        },
        Option(
          allDependencies.in(p) ++=
            semanticdbVersion.map { ver =>
              compilerPlugin(
                SourcegraphPlugin.autoImport.sourcegraphSemanticdb(
                  ver
                )
              )
            }.toSeq
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
      scalacOptions := {
        val old = scalacOptions.value
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
  ): Seq[(ProjectRef, Option[String], Option[String])] = for {
    p <- extracted.structure.allProjectRefs
    projectScalaVersion <- scalaVersion
      .in(p)
      .get(extracted.structure.data)
      .toList
    overriddenScalaVersion =
      if (
        projectScalaVersion.startsWith("2.11") &&
        Versions.semanticdbVersion(projectScalaVersion).isEmpty
      )
        Some("2.11.12")
      else None
    // For Scala 3 projects,
    semanticdbVersion = Versions
      .semanticdbVersion(overriddenScalaVersion.getOrElse(projectScalaVersion))
  } yield (p, semanticdbVersion, overriddenScalaVersion)
}
