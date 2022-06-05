addSbtPlugin(
  "com.sourcegraph" % "sbt-sourcegraph" % sys.props("plugin.version")
)

libraryDependencies += "com.sourcegraph" % "scip-semanticdb" % "0.8.0"
