import sbt.Keys._
import sbt._

object Testing {
  def settings = Seq(
    Test / fork := false,
    Test / testOptions += Tests.Cleanup(cl => {
      import scala.language.reflectiveCalls
      val c = cl.loadClass("Common$")
      type M = { def close(): Unit }
      val m: M = c.getField("MODULE$").get(null).asInstanceOf[M]
        m.close()
    }),
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "4.0.1",
      "org.slf4j" % "slf4j-simple" % "1.7.32").map(_ % Test)
  )
}
