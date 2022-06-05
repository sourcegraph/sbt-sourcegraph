package com.sourcegraph.sbtsourcegraph

import sbt.Keys._
import sbt._
import sbt.internal.sbtsourcegraph.Compat
import sbt.plugins.JvmPlugin

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import scala.collection.JavaConverters._

object SourcegraphPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin
  object autoImport {
    val sourcegraphUpload: TaskKey[Unit] =
      taskKey[Unit](
        "Task to upload the SCIP index to Sourcegraph to enable precise code intelligence."
      )
    val sourcegraphScip: TaskKey[File] =
      taskKey[File](
        "Task to generate a single  SCIP index for all SemanticDB files in this workspace."
      )
    val sourcegraphTargetRoots: TaskKey[List[String]] =
      taskKey[List[String]](
        "Task to generate a single  SCIP index for all SemanticDB files in this workspace."
      )
    val sourcegraphTargetRootsFile: TaskKey[File] =
      taskKey[File](
        "Task to generate a single  SCIP index for all SemanticDB files in this workspace."
      )
    val sourcegraphScipJavaVersion: SettingKey[String] =
      settingKey[String]("The version of the `scip-java` command-line tool.")
    val sourcegraphSemanticdbDirectories: TaskKey[List[File]] =
      taskKey[List[File]](
        "Task to compile all projects in this build and aggregate all SemanticDB directories."
      )
    val sourcegraphEndpoint: TaskKey[Option[String]] =
      taskKey[Option[String]](
        "URL of your Sourcegraph instance. By default, uploads to https://sourcegraph.com."
      )
    val sourcegraphCoursierBinary: TaskKey[String] =
      taskKey[String](
        "Binary name of the Coursier command-line tool. By default, Coursier is launched from a small binary that's embedded in resources."
      )
    val sourcegraphSrcBinary: SettingKey[String] =
      settingKey[String](
        "Binary name of the Sourcegraph command-line tool. By default, assumes the binary name 'src' is available on the $PATH."
      )
    val sourcegraphExtraUploadArguments: TaskKey[List[String]] =
      taskKey[List[String]](
        "Additional arguments to pass to `src code-intel upload`. Use this setting to specify flags like --commit, --repo, --github-token, --root or --upload-route."
      )
    val sourcegraphRoot: TaskKey[File] =
      taskKey[File](
        "The --root argument to the 'src code-intel upload' command. By default, uses root directory of this build."
      )
    val sourcegraphScalacTargetroot: TaskKey[File] =
      taskKey[File](
        "The directories where the semanticdb-scalac compiler plugin emits SemanticDB files."
      )
    val sourcegraphJavacTargetroot: TaskKey[Option[File]] =
      taskKey[Option[File]](
        "The directories where the semanticdb-javac compiler plugin emits SemanticDB files."
      )

    val Sourcegraph: Configuration =
      config("sourcegraph")

    val sourcegraphSemanticdb: ModuleID =
      sourcegraphSemanticdb(Versions.scalametaVersion)
    def sourcegraphSemanticdb(scalametaVersion: String): ModuleID =
      "org.scalameta" % "semanticdb-scalac" % scalametaVersion cross CrossVersion.full

  }

  import autoImport._

  override lazy val buildSettings: Seq[Def.Setting[_]] = List(
    sourcegraphScipJavaVersion := {
      scala.util.Properties
        .propOrElse("scip-java-version", Versions.semanticdbJavacVersion())
    },
    sourcegraphTargetRoots := {
      val directories =
        sourcegraphSemanticdbDirectories.all(anyProjectFilter).value
      val directoryArguments = directories.iterator.flatten
        .map(_.getAbsolutePath())
        .toList
        .distinct
      if (directoryArguments.isEmpty) {
        throw new TaskException(
          "Can't upload SCIP index to Sourcegraph because there are no SemanticDB directories. " +
            "To fix this problem, run the `sourcegraphEnable` command before `sourcegraphScip` like this: sbt sourcegraphEnable sourcegraphScip"
        )
      }
      directoryArguments
    },
    sourcegraphTargetRootsFile := {
      val roots = sourcegraphTargetRoots.value
      val out = (Sourcegraph / target).value / "targetroots.txt"
      Files.createDirectories(out.toPath().getParent())
      Files.write(
        out.toPath(),
        roots.asJava,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
      out
    },
    sourcegraphScip := {
      val out = (Sourcegraph / target).value / "index.scip"
      out.getParentFile.mkdirs()
      runProcess(
        sourcegraphCoursierBinary.value ::
          "launch" ::
          s"com.sourcegraph:scip-java_2.13:${sourcegraphScipJavaVersion.value}" ::
          "-M" ::
          "com.sourcegraph.scip_java.ScipJava" ::
          "--" ::
          "index-semanticdb" ::
          s"--output=$out" ::
          sourcegraphTargetRoots.value
      )
      out
    },
    sourcegraphUpload := {
      val streamsValue = streams.value
      if (System.getenv("GITHUB_TOKEN") == null) {
        streamsValue.log.warn(
          "sourcegraphUpload: skipping upload because the GITHUB_TOKEN environment variable is not defined. " +
            "To fix this problem, export the GITHUB_TOKEN environment variable according to the instructions " +
            "in https://github.com/sourcegraph/sbt-sourcegraph/blob/main/README.md"
        )
      }
      val in = sourcegraphScip.value
      val uploadCommand = List[Option[String]](
        Some(sourcegraphSrcBinary.value),
        sourcegraphEndpoint.value.map(url => s"--endpoint=$url"),
        Some("code-intel"),
        Some("upload"),
        Option(System.getenv("GITHUB_TOKEN"))
          .map(token => s"--github-token=$token"),
        Option(System.getenv("GITHUB_SHA"))
          .map(sha => s"--commit=$sha"),
        Some(s"--file=${in.getAbsolutePath}"),
        Some(s"--root=")
      )
      runProcess(
        uploadCommand.flatten ++ sourcegraphExtraUploadArguments.value
      )
    }
  )
  override lazy val globalSettings: Seq[Def.Setting[_]] = List(
    commands += SourcegraphEnable.command,
    sourcegraphSrcBinary := "src",
    sourcegraphEndpoint := None,
    sourcegraphExtraUploadArguments := Nil,
    sourcegraphRoot := (ThisBuild / baseDirectory).value,
    (Sourcegraph / target) := (ThisBuild / baseDirectory).value /
      "target" / "sbt-sourcegraph",
    sourcegraphCoursierBinary := createCoursierBinary(
      (Sourcegraph / target).value
    )
  )

  override lazy val projectSettings: Seq[Def.Setting[_]] = List(
    inConfig(Compile)(configSettings),
    inConfig(Test)(configSettings)
  ).flatten

  def configSettings: Seq[Def.Setting[_]] = List(
    sourcegraphUpload := sourcegraphUpload.value,
    sourcegraphScalacTargetroot := {
      val customDirectory = (for {
        option <- scalacOptions.value
        if option.startsWith("-P:semanticdb:targetroot:")
      } yield new File(
        option.stripPrefix("-P:semanticdb:targetroot:")
      )).lastOption
      customDirectory.getOrElse(classDirectory.value)
    },
    sourcegraphJavacTargetroot := {
      (for {
        option <- javacOptions.value
        if option.startsWith("-Xplugin:semanticdb")
        pluginOption <- option.split("\\s+")
        if pluginOption.startsWith("-targetroot:")
      } yield new File(pluginOption.stripPrefix("-targetroot:"))).lastOption
    },
    sourcegraphSemanticdbDirectories := {
      val javacTargetroot = sourcegraphJavacTargetroot.value
      val jars = fullClasspath.result.value match {
        case Value(value) => value.map(_.data)
        case other        => Nil
      }
      val results = List(
        javacTargetroot,
        Option(sourcegraphScalacTargetroot.value)
      ).flatten
        .map(f => f / "META-INF" / "semanticdb")
        .filter(_.isDirectory())
      results.headOption.foreach { dir =>
        if (jars.nonEmpty) {
          Files.write(
            dir.toPath.resolve("javacopts.txt"),
            List("-classpath", jars.mkString(File.pathSeparator)).asJava,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
          )
        }
      }
      results
    }
  )

  private val anyProjectFilter: ScopeFilter = ScopeFilter(
    inAnyProject,
    inConfigurations(Compile, Test)
  )

  private class TaskException(message: String)
      extends RuntimeException(message)
      with FeedbackProvidedException

  private def runProcess(command: List[String]): Unit = {
    val exit = scala.sys.process.Process(command).!
    if (exit != 0) {
      val commandSyntax = command.mkString(" ")
      throw new TaskException(
        s"'${command.head}' failed with exit code '$exit'. To reproduce this error, run the following command:\n$commandSyntax"
      )
    }
  }

  def createCoursierBinary(dir: File): String = {
    val out = dir / "coursier"
    if (!out.exists()) {
      val key = "/sbt-sourcegraph/coursier"
      val in = this.getClass().getResourceAsStream(key)
      if (in == null) {
        throw new NoSuchElementException(
          s"the resource '$key' does not exist. " +
            "To fix this problem, define the `sourcegraphCoursierBinary` setting."
        )
      }
      try {
        out.getParentFile().mkdirs()
        Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING)
      } finally {
        in.close()
      }
      out.setExecutable(true)
    }
    out.getAbsolutePath()
  }

  val relaxScalacOptionsConfigSettings: Seq[Def.Setting[_]] =
    Seq(
      (compile / scalacOptions) := {
        val options = (compile / scalacOptions).value
        options.filterNot { option =>
          scalacOptionsToRelax.exists(_.matcher(option).matches)
        }
      },
      incOptions := {
        val options = incOptions.value
        // maximize chance to get a zinc cache hit when running scalafix, even though we have
        // potentially added/removed scalacOptions for that specific invocation
        Compat.addIgnoredScalacOptions(
          options,
          scalacOptionsToRelax.map(_.pattern())
        )
      },
      manipulateBytecode := {
        // prevent storage of the analysis with relaxed scalacOptions - despite not depending explicitly on compile,
        // it is being triggered for parent configs/projects through evaluation of dependencyClasspath (TrackAlways)
        // in the scope where scalafix is invoked
        Compat.withHasModified(manipulateBytecode.value, false)
      }
    )

  private val scalacOptionsToRelax =
    List("-Xfatal-warnings", "-Werror", "-Wconf.*").map(_.r.pattern)
}
