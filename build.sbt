
name := """rdfshape"""

version := "1.0.1"

scalacOptions ++= Seq("-deprecation", "-feature")

lazy val root = (project in file(".")).enablePlugins(PlayScala).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "rdfshapeBuildinfo"
  )

// unmanagedSourceDirectories in Compile <+= twirlCompileTemplates.target

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
, "commons-io" % "commons-io" % "2.4"  
, "org.apache.jena" % "jena-arq" % "2.10.1" excludeAll(ExclusionRule(organization = "org.slf4j"))
, "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
, "es.weso" % "shexcala_2.11" % "0.5.6" excludeAll(ExclusionRule(organization = "org.slf4j")) 
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

herokuAppName in Compile := "rdfshape"

enablePlugins(JavaAppPackaging)