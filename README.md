# Json Validator

### Run application:

1. Run `./start.sh`

it would:
* compile application
* build Docker image
* and run application

### Implementation details:
Application is using `circe-json-schema` for schema validation, it provides errors accumulation

Endpoint GET /schema/SCHEMAID is providing response not in the same format as other two endpoints, because it wasn't requested, and it looked it might be designed for different audience, e.g. internal users

More generic resource management on connection between Http4s and ZIO would be beneficial