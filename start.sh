#!/bin/bash

sbt clean compile
docker-compose up -d db && docker ps
sbt clean docker
java -jar json-validator-api/target/scala-2.13/json-validator-api.jar