package com.github.tminglei.slickpg

import com.dimafeng.testcontainers.ForAllTestContainer
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.scalatest.Suite
import org.testcontainers.utility.DockerImageName

trait PostgresContainer extends ForAllTestContainer { self: Suite =>
  
  lazy val imageName: String = "postgres"
  lazy val imageTag: String = "11"
  
  override val container: PostgreSQLContainer = PostgreSQLContainer(
    dockerImageNameOverride = DockerImageName.parse(s"$imageName:$imageTag").asCompatibleSubstituteFor("postgres"),
    databaseName = "test",
    username = "postgres",
    password = "",
    mountPostgresDataToTmpfs = true
  ).configure { ctx =>
    // allow passwordless access for tests
    ctx.withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
    ctx.withUrlParam("user", "postgres")
  }
  
  def createExtension(name: String): Unit = {
    val createExtensionResult = container.execInContainer(
      "sh", "-c", s"psql test -c 'CREATE EXTENSION IF NOT EXISTS $name;' -U postgres;"
    )
    if(createExtensionResult.getExitCode != 0){
      println(createExtensionResult.toString)
    }
  }
  
  def createHstore(): Unit = createExtension("hstore")
  
  def createLtree(): Unit = createExtension("ltree")
  
  def createTrgm(): Unit = createExtension("pg_trgm")
  
  def createPostgis(): Unit = createExtension("postgis")
  
  override def afterStart(): Unit = {
    createHstore()
    createLtree()
    createTrgm()
    
    // only create the postgis extension when using the postgis container
    if(imageName == "postgis/postgis") createPostgis()
    
    super.afterStart()
  }
}
