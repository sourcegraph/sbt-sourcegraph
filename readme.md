# sbt-sourcegraph

This is a zero-dependency sbt plugin to help you enable precise code
intelligence for Scala projects on Sourcegraph.

- works with Scala 2.11, 2.12 and 2.13
- works with self-hosted Sourcegraph instances
- requires sbt v1.3 or newer

**Overview**:

- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Disable plugin for specific project](#disable-plugin-for-specific-project)
- [Known limitations](#known-limitations)

## Getting started

First, make sure you are using sbt v1.3 or newer. This plugin does not work with
older versions of sbt.

Next, add the sbt plugin to your build in `project/plugins.sbt`.

[![](https://index.scala-lang.org/sourcegraph/sbt-sourcegraph/latest.svg?color=blue)](https://github.com/sourcegraph/sbt-sourcegraph/releases)

```diff
  // project/plugins.sbt
+ addSbtPlugin("com.sourcegraph" % "sbt-sourcegraph" % "LATEST_VERSION")
```

Next, enable SemanticDB in `build.sbt`.

```diff
  // build.sbt
+ ThisBuild / semanticdbEnabled := true
  lazy val myproject1 = project
    .settings(
      ...
    )
  lazy val myproject2 = project
```

Optionally, enable SemanticDB on a per-project basis instead of via `ThisBuild`.

```diff
  // build.sbt
- ThisBuild / semanticdbEnabled := true
  lazy val myproject1 = project
    .settings(
      ...
+     semanticdbEnabled := true
    )
  lazy val myproject2 = project
```

Next, add a GitHub Actions workflow to your repository to configure your CI to
upload indexes on pull requests and merge into your main branch. See the section
on [other CI systems](#other-ci-systems) for how to configure this plugin if you
are not using GitHub Actions.

```sh
mkdir -p .github/workflows && \
  curl -L https://raw.githubusercontent.com/sourcegraph/sbt-sourcegraph/master/.github/workflows/sourcegraph.yml > .github/workflows/sourcegraph.yml
```

Feel free to adjust `sourcegraph.yml` to your needs, for example to disable
uploading on pull requests. Commit the new file and push it to GitHub to trigger
the upload job. Once the upload job completes, you should be able to observe
precise code intelligence on GitHub.

### Other CI systems

This plugin can be used with any CI system. In short, the installation steps
from [`sourcegraph.yml`](.github/workflows/sourcegraph.yml) document all of the
requirements to run this plugin.

First, install the following binaries to `$PATH`.

- https://github.com/sourcegraph/lsif-semanticdb as `lsif-semanticdb`
- https://github.com/sourcegraph/src-cli as `src`

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
- `sourcegraphLsifSemanticdbBinary: String`: path to the
  [`lsif-semanticdb`](https://github.com/sourcegraph/lsif-semanticdb/) binary.
  The `lsif-semanticdb` binary needs to be installed separately.
- `sourcegraphSrcBinary: String`: path to the
  [`src`](https://github.com/sourcegraph/src-cli) binary. The `src` binary needs
  to be installed separately.
- `sourcegraphExtraUploadArguments: List[String]`: additional arguments to use
  for the `src lsif upload` command. Run `src lsif upload --help` for example
  flags you may want to configure.
- `sourcegraphRoot: String`: root directory of this sbt build.

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
