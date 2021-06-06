import scala.language.postfixOps
// Centralized control of the application name/version
Global / version := "0.11"
Global / name := "rdfshape"
Global / cancelable := true
Global / apiURL := Some(url("https://github.com/weso/rdfshape-api"))

lazy val scala212               = "2.12.13"
lazy val scala213               = "2.13.5"
lazy val supportedScalaVersions = List(scala212, scala213)

// Lint-excluded keys
Global / excludeLintKeys ++= Set(
  ThisBuild / maintainer,
  rdfshape / reStartArgs,
  packageName
)

/* ------------------------------------------------------------------------- */

/* GITHUB INTEGRATION settings */
// "sbt-github-packages" plugin settings
// ThisBuild / githubOwner := "weso"
// ThisBuild / githubRepository := "shaclex"
// githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")

// "sbt-github-actions" plugin settings
val JavaCIVersion = "adopt@1.11"
ThisBuild / githubWorkflowJavaVersions := Seq(JavaCIVersion)

/* ------------------------------------------------------------------------- */

/* GROUPED SETTINGS */
// Shared dependencies for all modules.
lazy val sharedDependencies = Seq()

// Shared packaging settings for all modules.
lazy val packagingSettings = Seq(
  Compile / mainClass := Some("es.weso.rdfshape.Main"),
  assembly / mainClass := Some("es.weso.rdfshape.Main"),
  assembly / test := {},
  assembly / assemblyJarName := s"${(Global / name).value}.jar",
  // Output filename on "sbt-native-packager" tasks
  Universal / packageName := (Global / name).value
)

// Shared compilation settings for all modules.
// https://docs.scala-lang.org/overviews/compiler-options/index.html
lazy val compilationSettings = Seq(
  Compile / scalacOptions ++= Seq(
    "-deprecation",
    "-explaintypes",
    "-feature",
    "-language:_",
    "-unchecked",
    "-Yrangepos"
  )
)

// Scaladoc settings for docs generation. Run task "doc" or "server / doc".
// https://www.scala-sbt.org/1.x/docs/Howto-Scaladoc.html
/* https://github.com/scala/scala/blob/2.11.x/src/scaladoc/scala/tools/nsc/doc/Settings.scala */
lazy val scaladocSettings: Seq[Def.Setting[_]] = Seq(
  // Generate documentation on a separated "docs" folder
  Compile / doc / target := baseDirectory.value / "target" / "scaladoc",
  Compile / doc / scalacOptions ++= Seq(
    // Base source path
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath,
    // Link to GitHub source
    "-doc-source-url",
    scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    // Page title
    "-doc-title",
    "RDFShape API - Docs",
    // Docs version
    "-doc-version",
    version.value,
    // Docs footer
    "-doc-footer",
    "WESO Research Group - University of Oviedo",
    // Skip unnecessary source
    "-skip-packages",
    "org",
    // Other settings
    "-diagrams",
    "-implicits"
  ),
  // Do not generate docs when publishing binaries
  Compile / packageDoc / publishArtifact := false
)

// Shared publish settings for all modules.
lazy val publishSettings = Seq(
  maintainer := "info@weso.es",
  homepage := Some(url("https://github.com/weso/rdfshape-api")),
  licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/weso/rdfshape-api"),
      "scm:git:git@github.com:weso/rdfshape-api.git"
    )
  ),
  pomExtra := <developers>
    <developer>
      <id>WESO</id>
      <name>WESO Research Group</name>
      <url>https://github.com/weso</url>
    </developer>
    <developer>
      <id>labra</id>
      <name>Jose Emilio Labra Gayo</name>
      <url>http://www.example.com/jdoe</url>
      <organization>WESO</organization>
      <organizationUrl>https://github.com/weso</organizationUrl>
    </developer>
    <developer>
      <id>ulitol</id>
      <name>Eduardo Ulibarri Toledo</name>
      <url>http://www.example.com/jdoe</url>
      <organization>WESO</organization>
      <organizationUrl>https://github.com/weso</organizationUrl>
    </developer>
  </developers>,
  publishMavenStyle := true // generate POM, not ivy
)

// Aggregate resolver settings passed down to modules to resolve dependencies
// Helper to resolve dependencies from GitHub packages
lazy val resolverSettings = Seq(
  resolvers ++= Seq(
    //Resolver.githubPackages("weso"),
    Resolver.sonatypeRepo("snapshots")
  )
)

// Shared settings for the BuildInfo Plugin
// See https://github.com/sbt/sbt-buildinfo
lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    apiURL
  ),
  buildInfoPackage := "buildinfo"
)

/* ------------------------------------------------------------------------- */

/* PROJECT and MODULE settings */
// Root project: rdfshape
lazy val rdfshape = project
  .in(file("."))
  .aggregate(server)
  .dependsOn(server)
  .enablePlugins(
    BuildInfoPlugin,
    SbtNativePackager,
    JavaAppPackaging
  )
  .disablePlugins(RevolverPlugin)
  .settings(
    compilationSettings,
    packagingSettings,
    publishSettings,
    resolverSettings,
    scaladocSettings,
    sharedDependencies,
    buildInfoSettings
  )
  .settings(
    name := (Global / name).value,
    run / fork := true,
    trapExit := false,
    reStartArgs := Seq("--server"),
    crossScalaVersions := Nil,
    libraryDependencies ++= Seq(
      logbackClassic,
      scalaLogging
    )
  )

// Server project in /modules: server
lazy val server = project
  .in(file("modules/server"))
  .settings(
    compilationSettings,
    publishSettings,
    resolverSettings,
    scaladocSettings,
    sharedDependencies
  )
  .settings(
    name := s"${(Global / name).value}-server",
    run / fork := false,
    testFrameworks += MUnitFramework,
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      http4sDsl,
      http4sBlazeServer,
      http4sBlazeClient,
      http4sEmberClient,
      http4sCirce,
      scalatags,
      umlShaclex,
      wikibaserdf,
      any23_core,
      any23_api,
      any23_scraper,
      rdf4j_runtime,
      plantuml,
      graphvizJava,
      scalaj,
      utilsTest   % Test,
      munitEffect % Test,
      mongodb
    )
  )

lazy val MUnitFramework = new TestFramework("munit.Framework")

/* ------------------------------------------------------------------------- */

/* DEPENDENCY versions */
lazy val http4sVersion       = "1.0.0-M21"
lazy val catsVersion         = "2.5.0"
lazy val shexsVersion        = "0.1.91"
lazy val mongodbVersion      = "4.1.1"
lazy val utilsVersion        = "0.1.94"
lazy val umlShaclexVersion   = "0.0.82"
lazy val any23Version        = "2.2"
lazy val rdf4jVersion        = "2.2.4"
lazy val graphvizJavaVersion = "0.5.2"
lazy val logbackVersion      = "1.2.3"
lazy val loggingVersion      = "3.9.2"
lazy val munitVersion        = "0.7.23"
lazy val munitEffectVersion  = "1.0.2"
lazy val plantumlVersion     = "1.2021.5"
lazy val scalajVersion       = "2.4.2"
lazy val scalatagsVersion    = "0.7.0"

// Dependency modules
lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % http4sVersion
lazy val http4sBlazeServer =
  "org.http4s" %% "http4s-blaze-server" % http4sVersion
lazy val http4sBlazeClient =
  "org.http4s" %% "http4s-blaze-client" % http4sVersion
lazy val http4sEmberClient =
  "org.http4s" %% "http4s-ember-client" % http4sVersion
lazy val http4sCirce = "org.http4s" %% "http4s-circe" % http4sVersion

lazy val catsCore   = "org.typelevel" %% "cats-core"   % catsVersion
lazy val catsKernel = "org.typelevel" %% "cats-kernel" % catsVersion

lazy val wikibaserdf = "es.weso" %% "wikibaserdf" % shexsVersion

lazy val mongodb = "org.mongodb.scala" %% "mongo-scala-driver" % mongodbVersion

lazy val utilsTest  = "es.weso" %% "utilstest"  % utilsVersion
lazy val umlShaclex = "es.weso" %% "umlshaclex" % umlShaclexVersion

lazy val any23_core = "org.apache.any23" % "apache-any23-core" % any23Version
lazy val any23_api  = "org.apache.any23" % "apache-any23-api"  % any23Version
lazy val any23_scraper =
  "org.apache.any23.plugins" % "apache-any23-html-scraper" % "2.2"

lazy val rdf4j_runtime = "org.eclipse.rdf4j"        % "rdf4j-runtime" % rdf4jVersion
lazy val graphvizJava  = "guru.nidi"                % "graphviz-java" % graphvizJavaVersion
lazy val plantuml      = "net.sourceforge.plantuml" % "plantuml"      % plantumlVersion

lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
lazy val scalaLogging =
  "com.typesafe.scala-logging" %% "scala-logging" % loggingVersion

lazy val munit = "org.scalameta" %% "munit" % munitVersion
lazy val munitEffect =
  "org.typelevel" %% "munit-cats-effect-3" % munitEffectVersion

lazy val scalaj    = "org.scalaj"  %% "scalaj-http" % scalajVersion
lazy val scalatags = "com.lihaoyi" %% "scalatags"   % scalatagsVersion
