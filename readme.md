# sbt-sourcegraph

This is a zero-dependency sbt plugin to help you enable precise code
intelligence for Scala projects on Sourcegraph.

- works with Scala 2.11, 2.12 and 2.13
- works with self-hosted Sourcegraph instances
- requires sbt v1.4 or newer

**Overview**:

- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Disable plugin for specific project](#disable-plugin-for-specific-project)
- [Known limitations](#known-limitations)
- [Troubleshooting](#troubleshooting)

## Getting started

First, make sure you are using sbt v1.4 or newer. This plugin does not work with
older versions of sbt.

Next, add the sbt plugin to your build in `project/plugins.sbt`.

[![](https://index.scala-lang.org/sourcegraph/sbt-sourcegraph/latest.svg?color=blue)](https://github.com/sourcegraph/sbt-sourcegraph/releases)

```diff
  // project/plugins.sbt
+ addSbtPlugin("com.sourcegraph" % "sbt-sourcegraph" % "0.1.5")
```

Next, enable SemanticDB in `build.sbt` and use the latest version of SemanticDB.
[![](https://index.scala-lang.org/scalameta/scalameta/latest.svg?color=blue)](https://mvnrepository.com/artifact/org.scalameta/semanticdb-scalac)

```diff
  // build.sbt
+ ThisBuild / semanticdbEnabled := true
+ ThisBuild / semanticdbVersion := "4.4.7"
  lazy val myproject1 = project
    .settings(
      ...
    )
  lazy val myproject2 = project
```

Next, add a GitHub Actions workflow to your repository to configure your CI to
upload indexes on pull requests and merge into your main branch.

```sh
mkdir -p .github/workflows && \
  curl -L https://raw.githubusercontent.com/sourcegraph/sbt-sourcegraph/master/.github/workflows/sourcegraph.yml > .github/workflows/sourcegraph.yml
```

Optionally, adjust `sourcegraph.yml` to your needs. For example, you may want to
disable the upload job for pull requests and use Java 11.

```diff
  // .github/workslows/sourcegraph.yml
  on:
    push:
      branches:
        - main
-     pull_request:
  jobs:
    lsif:
      steps:
        - uses: olafurpg/setup-scala@v10
+         with:
+           java-version: adopt@1.11
        - uses: actions/setup-go@v2
          with:
            go-version: "1.15.6"
```

Commit the new file and push it to GitHub to trigger the upload job. Once the
upload job completes, you should be able to observe precise code intelligence on
GitHub.

### Other ways to enable SemanticDB

If you don't want to enable SemanticDB in `build.sbt`, you can do it a single
sbt session inside the upload CI job.

```sh
$ sbt \
    'set every semanticdbEnabled := true' \
    'set every semanticdbVersion := "LATEST_VERSION"' \
    sourcegraphUpload
```

If you have projects that don't work with SemanticDB, you can optionally enable
SemanticDB on a per-project basis instead of via `ThisBuild`.

```diff
  // build.sbt
- ThisBuild / semanticdbEnabled := true
  ThisBuild / semanticdbVersion := "LATEST_VERSION"
  lazy val myproject1 = project
    .settings(
      ...
+     semanticdbEnabled := true
    )
  lazy val myproject2 = project
```

### Other CI systems

This plugin can be used with any CI system. If you don't use GitHub Actions, you
can adjust the installation steps from
[`sourcegraph.yml`](.github/workflows/sourcegraph.yml) to work with your own CI
system.

First, install the `src` command-line tool
(https://github.com/sourcegraph/src-cli) to `$PATH`.

Next, create a GitHub access token following the instructions
[here](https://docs.sourcegraph.com/admin/external_service/github#github-api-token-and-access).

Finally, run `sbt sourcegraphUpload` with the GitHub access token available via
`GITHUB_TOKEN`.

```sh
export GITHUB_TOKEN="REPLACE_THIS_WITH_ACTUAL_TOKEN"
sbt sourcegraphUpload
```

## Configuration

**Environment variables**:

- (required) `GITHUB_TOKEN`: GitHub access token that's used to upload the LSIF
  index.
- (optional) `GITHUB_SHA`: the git commit sha that the LSIF index should be
  associated with on Sourcegraph.

**Tasks**:

- `sourcegraphLsif`: compiles all projects in the build and generates an LSIF
  index from the compiled SemanticDB files.
- `sourcegraphUpload`: uploads the LSIF index from `sourcegraphLsif` to
  Sourcegraph.

**Optional settings**:

- `sourcegraphEndpoint: String`: URL of the Sourcegraph instance.
- `sourcegraphCoursierBinary: String`: name of the `coursier` command-line
  interface. By default, sbt-sourcegraph launches coursier from a binary that's
  embedded in the resources.
- `sourcegraphSrcBinary: String`: path to the
  [`src`](https://github.com/sourcegraph/src-cli) binary. The `src` binary needs
  to be installed separately.
- `sourcegraphExtraUploadArguments: List[String]`: additional arguments to use
  for the `src lsif upload` command. Run `src lsif upload --help` for example
  flags you may want to configure.
- `sourcegraphRoot: String`: root directory of this sbt build.

**Removed settings**:

- (no longer used) `sourcegraphLsifSemanticdbBinary: String`: path to the
  [`lsif-semanticdb`](https://github.com/sourcegraph/lsif-semanticdb/) binary.

## Disable plugin for specific project

Use `.disablePlugins(SourcegraphPlugin))` to disable this plugin for a specific
project.

```diff
  // build.sbt

  lazy val myProject1 = project
  lazy val myProject2 = project
+   .disablePlugins(SourcegraphPlugin)
```

## Known limitations

Precise code intelligence for Scala is still under development. Below are some
known issues:

- Goto definition does not work for symbols from library dependencies.
  Navigation only works for symbols that are defined inside the repository.
- Hover tooltips don't show docstrings.
- Find references returns buggy results in some cases.
- `crossScalaVersions` is not supported. It's only possible to upload indexes
  for a single Scala version.

## Troubleshooting

### `NoSuchMethodError: sbt.Def$.ifS()`

The error below happens when you use this plugin with sbt v1.3 or older
versions.

```
java.lang.NoSuchMethodError: sbt.Def$.ifS(Lsbt/internal/util/Init$Initialize;Lsbt/internal/util/Init$Initialize;Lsbt/internal/util/Init$Initialize;)Lsbt/internal/util/Init$Initialize;
```

To fix this problem, upgrade to sbt v1.4.6 or newer.

```diff
  # project/build.properties
- sbt.version=1.3.10
+ sbt.version=1.4.6
```
