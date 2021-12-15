package com.sourcegraph.sbtsourcegraph

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import java.util.jar.JarFile

object Log4shellPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires = JvmPlugin
  object autoImport {
    val detectVulnerableLog4j = taskKey[Unit](
      "reports an error if we detect a vulnerable log4j dependency"
    )
  }
  import autoImport._
  override def projectSettings: Seq[Setting[_]] =
    List(Compile, Test).flatMap(c =>
      inConfig(c)(
        List(
          detectVulnerableLog4j := {
            val projectName = thisProject.value.id
            val l = streams.value.log
            dependencyClasspath.value.foreach { entry =>
              analyzeDependencyClasspathEntry(projectName, c, entry.data, l)
            }
          }
        )
      )
    )

  def analyzeDependencyClasspathEntry(
      projectName: String,
      config: Configuration,
      file: File,
      logger: Logger
  ): Unit = {
    if (file.getName().endsWith(".jar")) {
      val jar = new JarFile(file)
      val entries = jar.entries()
      try {
        while (entries.hasMoreElements()) {
          val element = entries.nextElement()
          if (
            element.getName() == "org/apache/logging/log4j/core/lookup/JndiLookup.class"
          ) {
            logger.error(
              s"CVE-2021-44228: the config '${config.name}' in the project '$projectName' depends on a vulnerable jar file ${file.absolutePath}"
            )
          }
        }
      } finally {
        jar.close()
      }
    }
  }
}
