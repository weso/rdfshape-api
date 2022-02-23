import scala.language.postfixOps
// Centralized control of the application's core settings
// See version in version.sbt
Global / name := "RDFShape API"    // Friendly app name
Global / packageName := "rdfshape" // Output filename of "sbt-native-packager" tasks
Global / cancelable := true
Global / apiURL := Some(url("https://github.com/weso/rdfshape-api"))
Global / scalaVersion := scala213

lazy val scala212               = "2.12.13"
lazy val scala213               = "2.13.6"
lazy val supportedScalaVersions = List(scala213)

// Lint-excluded keys
Global / excludeLintKeys ++= Set(
  name,
  ThisBuild / maintainer,
  rdfshape / reStartArgs
)

/* ------------------------------------------------------------------------- */

/* GITHUB INTEGRATION settings */
/* GROUPED SETTINGS */
// Shared dependencies for all modules.
lazy val sharedDependencies = Seq()
// Shared packaging settings for all modules.
lazy val packagingSettings = Seq(
  // Do not package logback files in .jar, they interfere with other logback
  // files in classpath
  Compile / packageBin / mappings ~= { project =>
    project.filter { case (file, _) =>
      val fileName = file.getName
      !(fileName.startsWith("logback") && (fileName.endsWith(".xml") || fileName
        .endsWith(".groovy")))
    }
  },
  Compile / mainClass := Some("es.weso.rdfshape.Main"),
  assembly / mainClass := Some("es.weso.rdfshape.Main"),
  assembly / test := {},
  assembly / assemblyJarName := s"${(Global / packageName).value}.jar",
  // Output filename on "sbt-native-packager" tasks
  Universal / packageName := (Global / packageName).value
)
/* ------------------------------------------------------------------------- */
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
/* https://github.com/scala/scala/blob/2.13.x/src/scaladoc/scala/tools/nsc/doc/Settings.scala */
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
    "org:buildinfo",
    // Other settings
    "-diagrams",
    "-implicits",
    "-private"
  ),
  // Need to generate docs to publish to oss
  Compile / packageDoc / publishArtifact := true
)
// Setup Mdoc + Docusaurus settings
lazy val mdocSettings = Seq(
  mdocVariables := Map(
    "APP_NAME"               -> (Global / name).value,
    "INNER_NAME"             -> name.value,
    "VERSION"                -> version.value,
    "WEBPAGE_URL"            -> "https://www.weso.es/rdfshape-api/",
    "API_URL"                -> "https://api.rdfshape.weso.es",
    "API_CONTAINER_REGISTRY" -> "https://github.com/orgs/weso/packages/container/package/rdfshape-api",
    "CLIENT_NAME"            -> "RDFShape Client",
    "CLIENT_REPO"            -> "https://github.com/weso/rdfshape-client/",
    "CLIENT_URL"             -> "https://rdfshape.weso.es/",
    "WESOLOCAL_URL"          -> "https://github.com/weso/wesolocal/wiki/RDFShape"
  ),
  mdocExtraArguments := Seq("--no-link-hygiene"),
  /* When creating/publishing the docusaurus site, update the dynamic mdoc and
   * the static scaladoc first */
  docusaurusCreateSite := docusaurusCreateSite
    .dependsOn(Compile / unidoc)
    .value,
  docusaurusPublishGhpages :=
    docusaurusPublishGhpages
      .dependsOn(Compile / unidoc)
      .value
)
// Unidoc settings, mirroring scaladoc settings
lazy val unidocSettings: Seq[Def.Setting[_]] = Seq(
  // Generate docs for the root project and the server module
  ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(rdfshape, server),
  // Dump docs into the website static part, to link them with docusaurus
  ScalaUnidoc / unidoc / target := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
  // When cleaning, remove unidoc generated docs as well
  cleanFiles += (ScalaUnidoc / unidoc / target).value,
  // Scalac options
  ScalaUnidoc / unidoc / scalacOptions ++= Seq(
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
    "org:buildinfo",
    // Other settings
    "-diagrams",
    "-implicits",
    "-private"
  )
)
// Shared publish settings for all modules.
lazy val publishSettings = Seq(
  organization := "es.weso",
  sonatypeProfileName := "es.weso",
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
    Resolver.DefaultMavenRepository,
    Resolver.sonatypeRepo("snapshots")
  )
)
// Shared settings for the BuildInfo Plugin
// See https://github.com/sbt/sbt-buildinfo
lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    packageName,
    version,
    scalaVersion,
    sbtVersion,
    apiURL
  ),
  buildInfoPackage := "buildinfo",
  buildInfoObject := "BuildInfo"
)
lazy val noPublishSettings = publish / skip := true
/* PROJECT and MODULE settings */
// Root project: rdfshape
lazy val rdfshape = project
  .in(file("."))
  .aggregate(server, docs)
  .dependsOn(server)
  .enablePlugins(
    BuildInfoPlugin,
    SbtNativePackager,
    JavaAppPackaging
  )
  .disablePlugins(RevolverPlugin)
  .settings(
    // Pre-existing settings
    compilationSettings,
    packagingSettings,
    publishSettings,
    resolverSettings,
    scaladocSettings,
    sharedDependencies,
    buildInfoSettings,
    // Custom settings
    name := (Global / packageName).value,
    moduleName := (Global / packageName).value,
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
    // Pre-existing settings
    compilationSettings,
    publishSettings,
    resolverSettings,
    scaladocSettings,
    sharedDependencies,
    // Custom settings
    name := s"${(Global / packageName).value}-server",
    moduleName := s"${(Global / packageName).value}-server",
    run / fork := false,
    testFrameworks += MUnitFramework,
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      http4sDsl,
      http4sBlazeServer,
      http4sBlazeClient,
      http4sEmberClient,
      http4sCirce,
      rho_swagger,
      umlShaclex,
      shexs,
      shaclex,
      any23_core,
      any23_api,
      any23_scraper,
      rdf4j_runtime,
      plantuml,
      graphvizJava,
      scalaj,
      wesoUtils   % Test,
      munitEffect % Test,
      mongo4catsCore,
      mongo4catsCirce
    )
  )

// "sbt-github-actions" plugin settings
lazy val JavaCIVersion  = "adopt@1.11"
lazy val ScalaCIVersion = "2.13.6"
ThisBuild / githubWorkflowJavaVersions := Seq(JavaCIVersion)
ThisBuild / githubWorkflowScalaVersions := Seq(ScalaCIVersion)

/* ------------------------------------------------------------------------- */
// Documentation project, for MDoc + Docusaurus documentation
lazy val docs = project
  .in(file("rdfshape-docs"))
  .dependsOn()
  .enablePlugins(MdocPlugin, DocusaurusPlugin, ScalaUnidocPlugin)
  .settings(
    // Pre-existing settings
    unidocSettings,
    mdocSettings,
    noPublishSettings,
    // Custom settings
    name := s"${(Global / packageName).value}-api-docs",
    moduleName := s"${(Global / packageName).value}-api-docs"
  )
lazy val MUnitFramework = new TestFramework("munit.Framework")
/* DEPENDENCY versions */
lazy val http4sVersion = "1.0.0-M30"
lazy val rhoVersion    = "0.23.0-M1"
lazy val catsVersion   = "2.7.0"
/* ------------------------------------------------------------------------- */
lazy val mongodbVersion      = "4.4.1"
lazy val mongo4catsVersion   = "0.4.5"
lazy val any23Version        = "2.4"
lazy val rdf4jVersion        = "3.7.4"
lazy val graphvizJavaVersion = "0.18.1"
lazy val logbackVersion      = "1.2.10"
lazy val scalaLoggingVersion = "3.9.4"
lazy val munitVersion        = "0.7.27"
lazy val munitEffectVersion  = "1.0.7"
lazy val plantumlVersion     = "1.2021.14"
lazy val scalajVersion       = "2.4.2"
// WESO dependencies
lazy val shaclexVersion    = "0.1.103-ult_0"
lazy val shexsVersion      = "0.1.108-ult_0"
lazy val umlShaclexVersion = "0.0.82"
lazy val wesoUtilsVersion  = "0.2.2"
// Dependency modules
lazy val http4sDsl = "org.http4s" %% "http4s-dsl" % http4sVersion
lazy val http4sBlazeServer =
  "org.http4s" %% "http4s-blaze-server" % http4sVersion
lazy val http4sBlazeClient =
  "org.http4s" %% "http4s-blaze-client" % http4sVersion
lazy val http4sEmberClient =
  "org.http4s" %% "http4s-ember-client" % http4sVersion
lazy val http4sCirce = "org.http4s"        %% "http4s-circe"       % http4sVersion
lazy val rho_swagger = "org.http4s"        %% "rho-swagger"        % rhoVersion
lazy val catsCore    = "org.typelevel"     %% "cats-core"          % catsVersion
lazy val catsKernel  = "org.typelevel"     %% "cats-kernel"        % catsVersion
lazy val mongodb     = "org.mongodb.scala" %% "mongo-scala-driver" % mongodbVersion
lazy val mongo4catsCore =
  "io.github.kirill5k" %% "mongo4cats-core" % mongo4catsVersion
lazy val mongo4catsCirce =
  "io.github.kirill5k" %% "mongo4cats-circe" % mongo4catsVersion
lazy val any23_core = "org.apache.any23" % "apache-any23-core" % any23Version
lazy val any23_api  = "org.apache.any23" % "apache-any23-api"  % any23Version
lazy val any23_scraper =
  "org.apache.any23.plugins" % "apache-any23-html-scraper" % "2.3"
lazy val rdf4j_runtime = "org.eclipse.rdf4j" % "rdf4j-runtime" % rdf4jVersion
lazy val graphvizJava  = "guru.nidi"         % "graphviz-java" % graphvizJavaVersion
//noinspection SbtDependencyVersionInspection
lazy val plantuml       = "net.sourceforge.plantuml" % "plantuml"        % plantumlVersion
lazy val logbackClassic = "ch.qos.logback"           % "logback-classic" % logbackVersion
lazy val scalaLogging =
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
lazy val munit = "org.scalameta" %% "munit" % munitVersion
lazy val munitEffect =
  "org.typelevel" %% "munit-cats-effect-3" % munitEffectVersion
lazy val scalaj = "org.scalaj" %% "scalaj-http" % scalajVersion
// WESO dependencies
lazy val shexs      = "es.weso" %% "shexs"      % shexsVersion
lazy val shaclex    = "es.weso" %% "shaclex"    % shaclexVersion
lazy val umlShaclex = "es.weso" %% "umlshaclex" % umlShaclexVersion
lazy val wesoUtils  = "es.weso" %% "utilstest"  % wesoUtilsVersion
