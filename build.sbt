ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.9"

addCommandAlias("fmt", "scalafmtSbt; scalafmtAll")

lazy val `json-validator` = (project in file("."))
  .aggregate(`json-validator-api`)

lazy val `domain` = project
  .settings(
    name := "domain",
    libraryDependencies ++=
      dependencies.zio ++
        dependencies.circe ++
        dependencies.enumeration
  )

lazy val `json-validator-api` = project
  .dependsOn(`domain`)
  .settings(
    name := "json-validator-api",
    libraryDependencies ++=
      dependencies.http4s
  )
  .enablePlugins(DockerPlugin)
  .settings(
    assembly / assemblyJarName := "json-validator-api.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") =>
        MergeStrategy.singleOrError
      case x                                                                            =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    docker := (docker dependsOn assembly).value,
    docker / dockerfile := {
      val artifact           = assembly.value
      val artifactTargetPath = s"/app/${artifact.name}"
      new Dockerfile {
        from("openjdk:8-jre")
        add(artifact, artifactTargetPath)
        entryPoint("java", "-jar", artifactTargetPath)
      }
    }
  )

