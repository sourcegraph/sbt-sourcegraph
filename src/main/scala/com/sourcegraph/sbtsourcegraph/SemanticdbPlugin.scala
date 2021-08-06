package com.sourcegraph.sbtsourcegraph

import sbt._
import scala.util.control.NonFatal

/** Helper to use sbt 1.3+ SemanticdbPlugin features when available */
object SemanticdbPlugin {

  // Copied from https://github.com/scalacenter/sbt-scalafix/blob/cdee753f15bde75d84d93c26695d14fc3ec964f8/src/main/scala/scalafix/internal/sbt/SemanticdbPlugin.scala
  val semanticdbEnabled: SettingKey[Boolean] = settingKey[Boolean]("")
  val semanticdbVersion: SettingKey[String] = settingKey[String]("")
  val semanticdbTargetRoot: SettingKey[File] = settingKey[File]("")

  def isAvailable(): Boolean = try {
    Class.forName("sbt.plugins.SemanticdbPlugin")
    true
  } catch {
    case NonFatal(_) =>
      false
  }

}
