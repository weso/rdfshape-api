import sbt._
import sbt.Keys._
import bintray.Plugin.bintraySettings
import bintray.Keys._

name := "rdfshape"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
   jdbc
  ,anorm
  ,cache
  , "org.specs2" %% "specs2" % "2.3.7" % "test" 
  , "org.slf4j" % "slf4j-simple" % "1.6.4"
  , "org.scalatest" % "scalatest_2.10" % "2.0.1-SNAP" % "test"
  , "commons-configuration" % "commons-configuration" % "1.7"
  , "commons-io" % "commons-io" % "2.4"
  , "com.typesafe" % "config" % "1.0.1"
  , "org.scala-lang" % "scala-compiler" % "2.10.2" 
  , "org.apache.jena" % "jena-arq" % "2.10.1" excludeAll(ExclusionRule(organization = "org.slf4j"))
  , "org.scalaz" % "scalaz-core_2.10" % "7.0.6" 
  , "es.weso" % "shexcala_2.10" % "0.0.12" excludeAll(ExclusionRule(organization = "org.slf4j"))  
  )     

seq(bintraySettings:_*)

play.Project.playScalaSettings

resolvers += "Bintray" at "http://dl.bintray.com/weso/weso-releases"
