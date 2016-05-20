
name := """rdfshape"""

version := "1.0.2"

scalacOptions ++= Seq("-deprecation", "-feature")

lazy val root = (project in file(".")).enablePlugins(PlayScala).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "rdfshapeBuildinfo"
  )

// unmanagedSourceDirectories in Compile <+= twirlCompileTemplates.target

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
//  anorm,
  cache,
  ws
, "commons-io" % "commons-io" % "2.4"  
, "postgresql" % "postgresql" % "9.1-901-1.jdbc4"

// ShExcala
//, "es.weso" % "shexcala_2.11" % "0.7.12" excludeAll(ExclusionRule(organization = "org.slf4j")) 
, "es.weso" % "shaclex_2.11" % "0.0.3" excludeAll(ExclusionRule(organization = "org.slf4j")) 
, "es.weso" % "shacl_tq_2.11" % "0.0.11" excludeAll(ExclusionRule(organization = "org.slf4j")) 

// , "org.scalatest" %% "scalatest" % "2.2.4" % "test"
, "org.scalatestplus" %% "play" % "1.2.0" % "test"
, "org.seleniumhq.selenium" % "selenium-java" % "2.35.0" % "test"
, "org.webjars" %% "webjars-play" % "2.3.0"
, "org.webjars" % "jquery" % "1.11.1"
, "org.webjars" % "jquery-ui" % "1.11.4"
, "org.webjars" % "jquery-ui-themes" % "1.11.4"
, "org.webjars" % "jquery-ui-touch-punch" % "0.2.3-2"
, "org.webjars" % "codemirror" % "4.3"
, "org.webjars" % "prettify" % "4-Mar-2013"
)

resolvers += "Bintray" at "http://dl.bintray.com/weso/weso-releases"

resolvers += Resolver.bintrayRepo("labra", "maven")

herokuAppName in Compile := "rdfshape"

enablePlugins(JavaAppPackaging)