import sbt._
import sbt.Keys._
import android.Keys._

object Build extends android.AutoBuild
{
	lazy val main = Project( "communicator", file( "." ) )
		.settings(
			autoScalaLibrary := false,
			exportJars := true,
			name := "communicator",
			organization := "com.taig.android",
			scalaVersion := "2.11.2",
			version := "1.0.6"
	)
}
