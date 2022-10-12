package json.validator.api

import cats.syntax.all._
import json.validator.api.routes.{JsonSchemaRegistry, JsonValidation}
import json.validator.domain.{
  CirceJsonSchemaValidator,
  InMemoryJsonSchemaRegistryService,
  JsonSchemaRegistryService,
  JsonValidationService
}
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio._
import zio.interop.catz._

object JsonValidatorApiApp extends ZIOAppDefault {

  type AppEffect[T] = RIO[JsonSchemaRegistryService with JsonValidationService with AppConfig, T]

  val routes: HttpRoutes[AppEffect] =
    JsonSchemaRegistry.routes.asInstanceOf[HttpRoutes[AppEffect]] <+> JsonValidation.routes.asInstanceOf[HttpRoutes[AppEffect]]

  val serve: AppEffect[Unit] =
    for {
      executor  <- ZIO.executor
      appConfig <- ZIO.service[AppConfig]
      server    <- BlazeServerBuilder[AppEffect]
                     .withExecutionContext(executor.asExecutionContext)
                     .bindHttp(appConfig.http.port, appConfig.http.host)
                     .withHttpApp(Router("/" -> routes).orNotFound)
                     .serve
                     .compile
                     .drain
    } yield server

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO.logInfo("Starting Json Validator service...") *>
      serve
        .provide(
          AppConfig.layer,
          InMemoryJsonSchemaRegistryService.layer,
          CirceJsonSchemaValidator.layer
        )
        .exitCode
}
