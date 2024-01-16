addSbtPlugin(
  "com.sourcegraph" % "sbt-sourcegraph" % sys.props("plugin.version")
)

libraryDependencies += "com.sourcegraph" % "scip-semanticdb" %
  sys.props("scip-java.version")
