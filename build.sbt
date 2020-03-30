lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.1"
lazy val supportedScalaVersions = List(scala213, scala212)

/*
scalafmt: {
  style = defaultWithAlign
  maxColumn = 150
  align.tokens = [
    { code = "=>", owner = "Case" }
    { code = "⇒", owner = "Case" }
    { code = "extends", owner = "Defn.(Class|Trait|Object)" }
    { code = "//", owner = ".*" }
    { code = "{", owner = "Template" }
    { code = "}", owner = "Template" }
    { code = ":=", owner = "Term.ApplyInfix" }
    { code = "++=", owner = "Term.ApplyInfix" }
    { code = "+=", owner = "Term.ApplyInfix" }
    { code = "%", owner = "Term.ApplyInfix" }
    { code = "%%", owner = "Term.ApplyInfix" }
    { code = "%%%", owner = "Term.ApplyInfix" }
    { code = "->", owner = "Term.ApplyInfix" }
    { code = "→", owner = "Term.ApplyInfix" }
    { code = "<-", owner = "Enumerator.Generator" }
    { code = "←", owner = "Enumerator.Generator" }
    { code = "=", owner = "(Enumerator.Val|Defn.(Va(l|r)|Def|Type))" }
  ]
}
 */

// Local dependencies 
// lazy val shaclexVersion        = "0.1.47" 
lazy val utilsVersion          = "0.1.67" // for utilsTest
lazy val umlShaclexVersion     = "0.0.51"


lazy val any23Version          = "2.2"
lazy val rdf4jVersion          = "2.2.4"

// Dependency versions
lazy val catsVersion           = "2.0.0"
lazy val commonsTextVersion    = "1.7"
lazy val circeVersion          = "0.12.0-M4"
lazy val graphvizJavaVersion   = "0.5.4"
lazy val http4sVersion         = "0.21.2"
lazy val jgraphtVersion        = "1.3.1"
lazy val logbackVersion        = "1.2.3"
lazy val loggingVersion        = "3.9.2"
lazy val plantumlVersion       = "1.2017.12"
lazy val scalacheckVersion     = "1.13.5"
lazy val scalacticVersion      = "3.0.8"
lazy val scalaGraphVersion     = "1.11.5"
lazy val scalajVersion         = "2.4.2"
lazy val scalaTestVersion      = "3.0.8"
lazy val scalatagsVersion      = "0.7.0"
lazy val scallopVersion        = "3.3.1"
lazy val seleniumVersion       = "2.35.0"
lazy val silencerVersion       = "1.4.2"
lazy val typesafeConfigVersion = "1.3.4"

// WebJars
lazy val jqueryVersion         = "3.4.1"
lazy val bootstrapVersion      = "4.3.1"

// Compiler plugin dependency versions
lazy val scalaMacrosVersion   = "2.1.1"

// Dependency modules
lazy val catsCore          = "org.typelevel"              %% "cats-core"           % catsVersion
lazy val catsKernel        = "org.typelevel"              %% "cats-kernel"         % catsVersion
lazy val catsMacros        = "org.typelevel"              %% "cats-macros"         % catsVersion
lazy val circeCore         = "io.circe"                   %% "circe-core"          % circeVersion
lazy val circeGeneric      = "io.circe"                   %% "circe-generic"       % circeVersion
lazy val circeParser       = "io.circe"                   %% "circe-parser"        % circeVersion
lazy val graphvizJava      = "guru.nidi"                  % "graphviz-java"       % graphvizJavaVersion
lazy val http4sDsl         = "org.http4s"                 %% "http4s-dsl"          % http4sVersion
lazy val http4sBlazeServer = "org.http4s"                 %% "http4s-blaze-server" % http4sVersion
lazy val http4sBlazeClient = "org.http4s"                 %% "http4s-blaze-client" % http4sVersion
lazy val http4sCirce       = "org.http4s"                 %% "http4s-circe"        % http4sVersion
lazy val http4sTwirl       = "org.http4s"                 %% "http4s-twirl"        % http4sVersion
lazy val logbackClassic    = "ch.qos.logback"             % "logback-classic"      % logbackVersion
lazy val plantuml          = "net.sourceforge.plantuml"   % "plantuml"             % plantumlVersion
lazy val scalaLogging      = "com.typesafe.scala-logging" %% "scala-logging"       % loggingVersion
lazy val scallop           = "org.rogach"                 %% "scallop"             % scallopVersion
lazy val scalactic         = "org.scalactic"              %% "scalactic"           % scalacticVersion
lazy val scalacheck        = "org.scalacheck"             %% "scalacheck"          % scalacheckVersion
lazy val scalaj            = "org.scalaj"                 %% "scalaj-http"         % scalajVersion
lazy val scalaTest         = "org.scalatest"              %% "scalatest"           % scalaTestVersion
lazy val scalatags         = "com.lihaoyi"                %% "scalatags"           % scalatagsVersion
lazy val selenium          = "org.seleniumhq.selenium"    % "selenium-java"        % seleniumVersion
lazy val umlShaclex        = "es.weso"                    %% "umlshaclex"          % umlShaclexVersion
lazy val utilsTest         = "es.weso"                    %% "utilstest"           % utilsVersion

lazy val any23_core        = "org.apache.any23"           % "apache-any23-core"    % any23Version
lazy val any23_api         = "org.apache.any23"           % "apache-any23-api"     % any23Version
lazy val any23_scraper     = "org.apache.any23.plugins"   % "apache-any23-html-scraper" % "2.2"
lazy val rdf4j_runtime     = "org.eclipse.rdf4j"          % "rdf4j-runtime"        % rdf4jVersion

lazy val jquery            = "org.webjars"                % "jquery"               % jqueryVersion
lazy val bootstrap         = "org.webjars"                % "bootstrap"            % bootstrapVersion


// Compiler plugin modules
lazy val scalaMacrosParadise = "org.scalamacros"      % "paradise"        % scalaMacrosVersion cross CrossVersion.full
//lazy val simulacrum          = "com.github.mpilquist" %% "simulacrum"     % simulacrumVersion
//lazy val kindProjector       = "org.spire-math"       %% "kind-projector" % kindProjectorVersion

lazy val rdfshape = project
  .in(file("."))
  .enablePlugins(
    ScalaUnidocPlugin,
    SbtNativePackager,
    WindowsPlugin,
    JavaAppPackaging,
    DockerPlugin
  )
  .disablePlugins(RevolverPlugin)
  .aggregate(server)
  .dependsOn(server)
  .settings(
    dockerExposedPorts ++= Seq(80),
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(noDocProjects: _*),
    libraryDependencies ++= Seq(
      logbackClassic,
      scalaLogging,
      scallop,
      compilerPlugin("com.github.ghik" %% "silencer-plugin" % silencerVersion),
      "com.github.ghik" %% "silencer-lib" % silencerVersion % Provided      
    ),
    cancelable in Global      := true,
    fork                      := true,
    reStartArgs               := Seq("--server"),
    crossScalaVersions := supportedScalaVersions,
    // parallelExecution in Test := false
  ).settings(commonSettings, packagingSettings, publishSettings, ghPagesSettings, wixSettings)

lazy val server = project
  .in(file("modules/server"))
  .enablePlugins(SbtTwirl, BuildInfoPlugin)
  .settings(commonSettings, publishSettings)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "es.weso.rdfshape.buildinfo" 
  )
  .settings(
    libraryDependencies ++= Seq(
      http4sDsl,
      http4sBlazeServer,
      http4sBlazeClient,
      http4sCirce,
      http4sTwirl,
      scalatags,
      selenium,
      umlShaclex,
      any23_core, any23_api, any23_scraper,
      rdf4j_runtime,
      plantuml,
      graphvizJava,
      scalaj,
      utilsTest % Test, 
      // webJars
      jquery,
      bootstrap
    ),
    crossScalaVersions := supportedScalaVersions,
  )

/* ********************************************************
 ******************** Grouped Settings ********************
 **********************************************************/

lazy val noDocProjects = Seq[ProjectReference]()

lazy val noPublishSettings = Seq(
//  publish := (),
//  publishLocal := (),
  publishArtifact := false
)

lazy val sharedDependencies = Seq(
  libraryDependencies ++= Seq(
    scalactic,
    scalaTest % Test
  )
)

lazy val packagingSettings = Seq(
  mainClass in Compile        := Some("es.weso.rdfshape.Main"),
  mainClass in assembly       := Some("es.weso.rdfshape.Main"),
  test in assembly            := {},
  assemblyJarName in assembly := "rdfshape.jar",
  packageSummary in Linux     := name.value,
  packageSummary in Windows   := name.value,
  packageDescription          := name.value
)

lazy val compilationSettings = Seq(
  scalaVersion := "2.13.1",
  // format: off
  scalacOptions ++= Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.  "-encoding", "UTF-8",
    "-language:_",
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
//    "-Xfuture",                          // Turn on future language features.
//    "-Xlint",
    "-Yrangepos",
//    "-Ylog-classpath",
//    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver
//    "-Ywarn-dead-code",                  // Warn when dead code is identified.
//    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
//    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
//    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
//    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
//    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
//    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
//    "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
//    "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
//    "-Ywarn-unused:locals",              // Warn if a local definition is unused.
//    "-Ywarn-unused:params",              // Warn if a value parameter is unused.
//    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
//    "-Ywarn-unused:privates",            // Warn if a private member is unused.
//    "-Ywarn-value-discard",              // Warn when non-Unit expression results are unused.
//    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
//    "-Ypartial-unification",
)
)



lazy val wixSettings = Seq(
  wixProductId        := "39b564d5-d381-4282-ada9-87244c76e14b",
  wixProductUpgradeId := "6a710435-9af4-4adb-a597-98d3dd0bade1"
// The same numbers as in the docs?
// wixProductId := "ce07be71-510d-414a-92d4-dff47631848a",
// wixProductUpgradeId := "4552fb0e-e257-4dbd-9ecb-dba9dbacf424"
)

lazy val ghPagesSettings = Seq(
  git.remoteRepo := "git@github.com:labra/rdfshape.git"
)

lazy val commonSettings = compilationSettings ++ sharedDependencies ++ Seq(
  organization := "es.weso",
  resolvers ++= Seq(
    Resolver.bintrayRepo("labra", "maven"),
    Resolver.bintrayRepo("weso", "weso-releases"),
    Resolver.sonatypeRepo("snapshots")
  )
)

lazy val publishSettings = Seq(
  maintainer      := "Jose Emilio Labra Gayo <labra@uniovi.es>",
  homepage        := Some(url("https://github.com/labra/rdfshape")),
  licenses        := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  scmInfo         := Some(ScmInfo(url("https://github.com/labra/rdfshape"), "scm:git:git@github.com:labra/rdfshape.git")),
  autoAPIMappings := true,
  apiURL          := Some(url("http://labra.github.io/rdfshape/latest/api/")),
  pomExtra        := <developers>
                       <developer>
                         <id>labra</id>
                         <name>Jose Emilio Labra Gayo</name>
                         <url>https://github.com/labra/</url>
                       </developer>
                     </developers>,
  scalacOptions in doc ++= Seq(
    "-diagrams-debug",
    "-doc-source-url",
    scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath",
    baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-diagrams",
  ),
  publishMavenStyle              := true,
  bintrayRepository in bintray   := "weso-releases",
  bintrayOrganization in bintray := Some("weso")
)

// silence all warnings on autogenerated files
scalacOptions += "-P:silencer:pathFilters=target/.*" 
// Make sure you only exclude warnings for the project directories, i.e. make builds reproducible
scalacOptions += s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}"
