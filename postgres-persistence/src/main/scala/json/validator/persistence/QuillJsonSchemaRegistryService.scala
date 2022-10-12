package json.validator.persistence

import cats.implicits._
import io.circe.{Json, JsonObject}
import io.circe.parser.decode
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import json.validator.domain.JsonSchemaRegistryService
import json.validator.domain.model.DomainServiceError.{InternalServerError, NotFoundError}
import json.validator.domain.model.{DomainServiceError, JsonSchema}
import json.validator.persistence.QuillJsonSchemaRegistryService.dataSourceLayer
import zio.{IO, TaskLayer, ZIO, ZLayer}

class QuillJsonSchemaRegistryService(quill: Quill.Postgres[SnakeCase]) extends JsonSchemaRegistryService {

  import quill._

  private implicit val jsonEncoder: MappedEncoding[JsonObject, String] =
    MappedEncoding[JsonObject, String](Json.fromJsonObject(_).noSpaces)

  private implicit val jsonDecoder: MappedEncoding[String, JsonObject] =
    MappedEncoding[String, JsonObject](decode[JsonObject](_).valueOr(throw _))

  override def register(schema: JsonSchema): IO[DomainServiceError, Unit] =
    insertOrIgnore(schema).mapError(er => InternalServerError(er.getMessage)).unit

  override def get(id: JsonSchema.Id): IO[DomainServiceError, JsonObject] =
    retrieveById(id)
      .mapError(er => InternalServerError(er.getMessage))
      .flatMap(jsonSchemas => ZIO.fromEither(jsonSchemas.headOption.map(_.schema).toRight(NotFoundError(id))))

  private val schemasTable = quote(querySchema[JsonSchema]("schema.schemas"))

  // current model is simple enough that doesn't need separate persistence model
  private def insertOrIgnore(entity: JsonSchema) =
    run(
      schemasTable
        .insertValue(lift(entity))
        .onConflictIgnore(_.id)
    ).provide(dataSourceLayer)

  private def retrieveById(id: JsonSchema.Id) =
    run(
      schemasTable
        .filter(_.id == lift(id))
    ).provide(dataSourceLayer)
}

object QuillJsonSchemaRegistryService {
  private val dataSourceLayer = Quill.DataSource
    .fromPrefix("db")
    .tapError(error => ZIO.logError(error.getMessage))
  private val quillLayer = Quill.Postgres.fromNamingStrategy(SnakeCase)
  private val serviceLayer = ZLayer.fromFunction(new QuillJsonSchemaRegistryService(_))

  val layer: TaskLayer[JsonSchemaRegistryService] =
    ZLayer.make[JsonSchemaRegistryService](dataSourceLayer, quillLayer, serviceLayer)
}
