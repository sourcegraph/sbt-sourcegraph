import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import scala.sys.process._
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object SourcegraphPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin
  object autoImport {
    val sourcegraphUpload: TaskKey[Unit] =
      taskKey[Unit](
        "Task to upload the LSIF index to Sourcegraph to enable precise code intelligence."
      )
    val sourcegraphLsif: TaskKey[File] =
      taskKey[File](
        "Task to generate a single LSIF index for all SemanticDB files in this workspace."
      )
    val sourcegraphSemanticdbDirectories: TaskKey[List[File]] =
      taskKey[List[File]](
        "Task to compile all projects in this build and aggregate all SemanticDB directories. Returns an empty list when `semanticdbEnabled := false`."
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
        "Additional arguments to pass to `src lsif upload`. Use this setting to specify flags like --commit, --repo, --github-token, --root or --upload-route."
      )
    val sourcegraphRoot: TaskKey[File] =
      taskKey[File](
        "The --root argument to the 'src lsif upload' command. By default, uses root directory of this build."
      )
    val sourcegraphJavacTargetroot: TaskKey[Option[File]] =
      taskKey[Option[File]](
        "The directories where the semanticdb-javac compiler plugin emits SemanticDB files."
      )

    val Sourcegraph: Configuration =
      config("sourcegraph")

  }
  import autoImport._

  override lazy val globalSettings: Seq[Def.Setting[_]] = List(
    sourcegraphSrcBinary := "src",
    sourcegraphEndpoint := None,
    sourcegraphExtraUploadArguments := Nil,
    sourcegraphRoot := (ThisBuild / baseDirectory).value,
    Sourcegraph / target := (ThisBuild / baseDirectory).value /
      "target" / "sbt-sourcegraph",
    sourcegraphCoursierBinary := {
      val out = (Sourcegraph / target).value / "coursier"
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
    },
    sourcegraphLsif := {
      val out = (Sourcegraph / target).value / "dump.lsif"
      out.getParentFile.mkdirs()
      val directories =
        sourcegraphSemanticdbDirectories.all(anyProjectFilter).value
      val directoryArguments = directories.iterator.flatten
        .map(_.getAbsolutePath())
        .toList
      if (directoryArguments.isEmpty) {
        throw new TaskException(
          "Can't upload LSIF index to Sourcegraph because there are no SemanticDB directories. " +
            "To fix this problem, add the setting `semanticdbEnabled := true` to build.sbt and try running this command again."
        )
      }
      runProcess(
        sourcegraphCoursierBinary.value ::
          "launch" ::
          "--contrib" ::
          "lsif-java" ::
          "--" ::
          "index-semanticdb" ::
          s"--output=$out" ::
          directoryArguments
      )
      out
    },
    sourcegraphUpload := {
      if (System.getenv("GITHUB_TOKEN") == null) {
        streams.value.log.warn(
          "sourcegraphUpload: skipping upload because the GITHUB_TOKEN environment variable is not defined. " +
            "To fix this problem, export the GITHUB_TOKEN environment variable according to the instructions " +
            "in https://github.com/sourcegraph/sbt-sourcegraph/blob/main/README.md"
        )
      } else {
        val in = sourcegraphLsif.value
        val uploadCommand = List[Option[String]](
          Some(sourcegraphSrcBinary.value),
          sourcegraphEndpoint.value.map(url => s"--endpoint=$url"),
          Some("lsif"),
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
    }
  )

  override lazy val projectSettings: Seq[Def.Setting[_]] = List(
    inConfig(Compile)(configSettings),
    inConfig(Test)(configSettings)
  ).flatten

  def configSettings: Seq[Def.Setting[_]] = List(
    sourcegraphUpload := sourcegraphUpload.value,
    sourcegraphJavacTargetroot := {
      (for {
        option <- javacOptions.value
        if option.startsWith("-Xplugin:semanticdb-javac")
        pluginOption <- option.split("\\s+")
        if pluginOption.startsWith("-targetroot:")
      } yield new File(pluginOption.stripPrefix("-targetroot:"))).lastOption
    },
    sourcegraphSemanticdbDirectories := {
      if (!semanticdbEnabled.value) {
        streams.value.log.warn(
          s"${name.value}: " +
            s"Skipping LSIF upload because semanticdbEnabled.value is false. " +
            "To fix this problem, set the semanticdbEnabled setting to true."
        )
        Nil
      } else {
        val javacTargetroot = sourcegraphJavacTargetroot.value
        val _ = fullClasspath.value
        List(
          javacTargetroot,
          Option(semanticdbTargetRoot.value)
        ).flatten
          .map(f => f / "META-INF" / "semanticdb")
          .filter(_.isDirectory())
      }
    }
  )

  private val anyProjectFilter: ScopeFilter = ScopeFilter(
    inAnyProject,
    inAnyConfiguration,
    inTasks(sourcegraphSemanticdbDirectories)
  )

  private class TaskException(message: String)
      extends RuntimeException(message)
      with FeedbackProvidedException

  private def runProcess(command: List[String]): Unit = {
    val exit = Process(command).!
    if (exit != 0) {
      val commandSyntax = command.mkString(" ")
      throw new TaskException(
        s"'${command.head}' failed with exit code '$exit'. To reproduce this error, run the following command:\n$commandSyntax"
      )
    }
  }

}
