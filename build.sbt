
name := """rdfshape"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
, "commons-io" % "commons-io" % "2.4"  
, "org.apache.jena" % "jena-arq" % "2.10.1" excludeAll(ExclusionRule(organization = "org.slf4j"))
, "es.weso" % "shexcala_2.11" % "0.1.3" excludeAll(ExclusionRule(organization = "org.slf4j"))  
)

resolvers += "Bintray" at "http://dl.bintray.com/weso/weso-releases"
