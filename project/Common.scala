import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

//import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys._

object Common extends AutoPlugin {
  //import com.typesafe.tools.mima.core._

  override def trigger = allRequirements
  override def requires = JvmPlugin

  val previousVersion = "0.12.1"

  private val silencerVer = Def.setting[String] {
    "1.7.5"
  }

  val playVersion = settingKey[String]("Play version")
  val playDirs = settingKey[Seq[String]]("Play source directory")
  import Compiler.{ playLower, playUpper }

  val useShaded = settingKey[Boolean](
    "Use ReactiveMongo-Shaded (see system property 'reactivemongo.shaded')")

  val driverVersion = settingKey[String]("Version of the driver dependency")

  override def projectSettings = Compiler.settings ++ Seq(
    useShaded := sys.env.get("REACTIVEMONGO_SHADED").fold(true)(_.toBoolean),
    driverVersion := {
      val v = (ThisBuild / version).value
      val suffix = {
        if (useShaded.value) "" // default ~> no suffix
        else "-noshaded"
      }

      v.span(_ != '-') match {
        case (a, b) => s"${a}${suffix}${b}"
      }
    },
    version ~= { ver =>
      sys.env.get("RELEASE_SUFFIX") match {
        case Some(suffix) => ver.split("-").toList match {
          case major :: Nil =>
            s"${major}-${suffix}"

          case vs @ _ =>
            ((vs.init :+ suffix) ++ vs.lastOption.toList).mkString("-")
        }

        case _ => ver
      }
    },
    organization := "org.reactivemongo",
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("staging"),
      Resolver.typesafeRepo("releases")),
    playVersion := {
      sys.env.get("PLAY_VERSION").getOrElse {
        if (scalaBinaryVersion.value == "2.11") playLower
        else playUpper
      }
    },
    playDirs := {
      val v = playVersion.value

      if (v startsWith "2.5") Seq("play-2.5-", "play-2.7-", "play-2.9-")
      else if (v startsWith "2.6") Seq("play-2.6+", "play-2.7-", "play-2.9-")
      else if (v startsWith "2.9") Seq("play-2.6+", "play-2.7+", "play-2.9+")
      else Seq("play-2.6+", "play-2.7+", "play-2.9-")
    },
    Compile / unmanagedSourceDirectories ++= playDirs.value.map { dir =>
      (Compile / sourceDirectory).value / dir
    },
    Test / unmanagedSourceDirectories ++= playDirs.value.map { dir =>
      (Test / sourceDirectory).value / dir
    },
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "4.10.6",
      "org.slf4j" % "slf4j-simple" % "1.7.32").map(_ % Test),
    libraryDependencies ++= Seq(
      compilerPlugin(
        ("com.github.ghik" %% "silencer-plugin" % silencerVer.value).
          cross(CrossVersion.full)),
      ("com.github.ghik" %% "silencer-lib" % silencerVer.value % Provided).
        cross(CrossVersion.full)),
    // mimaDefaultSettings
    mimaFailOnNoPrevious := false,
    mimaPreviousArtifacts := {
      if (scalaBinaryVersion.value != "2.13") {
        Set(organization.value %% moduleName.value % previousVersion)
      } else {
        Set.empty[ModuleID]
      }
    }
  ) ++ Publish.settings
}
