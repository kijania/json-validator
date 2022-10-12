package json.validator.api

import com.typesafe.config.{Config => TypesafeConfig}
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}
import zio.{TaskLayer, ZIO, ZLayer}

import scala.util.Try

case class AppConfig(
    http: HttpConfig,
    db: TypesafeConfig
) {
  override def toString: String = {
    def readDBConfig(key: String): String =
      Try(db.getString(key)).getOrElse[String](s"Failed to read key: $key from TypesafeConfig")

    s"""db: host = ${readDBConfig("dataSource.serverName")}
       |port = ${readDBConfig("dataSource.portNumber")}
       |http = $http
       |""".stripMargin
  }
}

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
