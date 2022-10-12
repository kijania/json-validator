package json.validator.api

import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.{TaskLayer, ZIO, ZLayer}

import scala.util.Try

case class AppConfig(
    http: HttpConfig
)

case class HttpConfig(
    host: String,
    port: Int
)

object HttpConfig {
  implicit val httpConfigReader: ConfigReader[HttpConfig] = deriveReader
}

object AppConfig {
  implicit val reader: ConfigReader[AppConfig] = deriveReader

  val layer: TaskLayer[AppConfig] = ZLayer(
    ZIO
      .fromTry(Try(ConfigSource.default.loadOrThrow[AppConfig]))
      .tapBoth(
        error => ZIO.logError(s"Failed to load config due to ${error.getMessage}"),
        config => ZIO.logInfo(s"Read config: $config")
      )
  )
}
