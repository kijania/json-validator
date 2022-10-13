# Json Validator

### Run application:

1. Run `./start.sh`

it would:
* compile application
* build Docker application image
* run postgresql in your local docker
* and run application

2. Create schema manually (until there is no automated data migration) in database

* `psql -h localhost -p 25432 -U postgres -d postgres`

* with password: `password`

* and run `CREATE SCHEMA schema; CREATE TABLE schema.schemas (id VARCHAR(255) NOT NULL, schema VARCHAR(2000) NOT NULL, PRIMARY KEY(id) );`

### Stop postgresql container:

Run: `./stop.sh`

### Restart application (without loosing previously uploaded JSON schemas):

Run: `./restart.sh`

it would:
* restart application

it won't:
* start Postgres database, so previously uploaded JSON schemas won't be loosed
* recompile and rebuild application if there are changes, as it potentially might raise conflict with the database schema

### Run tests, format code and build docker image:

1. Run `./ci.sh`

### Implementation details:
Application is using `circe-json-schema` for schema validation, it provides errors accumulation

JSON schema is currently encoded in database as a varchar what brings many limitations and should be changed to jsonb with better endcoding in the code

More generic resource management on connection between Http4s and ZIO would be beneficial

Extracting one common HTTP Error Mapper for all routes would be beneficial.

Endpoint GET /schema/SCHEMAID is providing response not in the same format as other two endpoints, because it wasn't requested, and it looked it might be designed for different audience, e.g. internal users

Database migration is missing and integration or embedded postgres tests which covers the persistence layer