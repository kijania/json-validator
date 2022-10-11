import sbt._

object dependencies {
  lazy val zio: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio"              % versions.ZIO,
    "dev.zio" %% "zio-interop-cats" % versions.ZioInterop
  )

  lazy val http4s: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-core"         % versions.Http4s,
    "org.http4s" %% "http4s-dsl"          % versions.Http4s,
    "org.http4s" %% "http4s-circe"        % versions.Http4s,
    "org.http4s" %% "http4s-blaze-server" % versions.Http4sServer
  )

  lazy val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core"        % versions.Circe,
    "io.circe" %% "circe-generic"     % versions.Circe,
    "io.circe" %% "circe-literal"     % versions.Circe,
    "io.circe" %% "circe-parser"      % versions.Circe,
    "io.circe" %% "circe-json-schema" % versions.CirceSchema
  )

  lazy val enumeration: Seq[ModuleID] = Seq(
    "com.beachape" %% "enumeratum"       % versions.Enumeration,
    "com.beachape" %% "enumeratum-circe" % versions.Enumeration
  )

  lazy val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest"         % versions.ScalaTest,
    "dev.zio"       %% "zio-test"          % versions.ZIO,
    "dev.zio"       %% "zio-test-sbt"      % versions.ZIO,
    "dev.zio"       %% "zio-test-magnolia" % versions.ZIO
  ).map(_ % Test)
}
