package org.example

//import java.sql.DriverManager
import java.sql.Driver

import slick.jdbc.MySQLProfile.api._
import com.dimafeng.testcontainers.{ContainerDef, ForAllTestContainer, SingleContainer}
import org.example.mysql.HelloSlick.connectToDB
import org.scalatest.flatspec.{AnyFlatSpec, AsyncFlatSpec}
import org.testcontainers.utility.DockerImageName
import org.testcontainers.containers.{MySQLContainer => JavaMySQLContainer}
import org.testcontainers.containers.{JdbcDatabaseContainer => JavaJdbcDatabaseContainer}

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

trait JdbcDatabaseContainer { self: SingleContainer[_ <: JavaJdbcDatabaseContainer[_]] =>

  def driverClassName: String = underlyingUnsafeContainer.getDriverClassName

  def jdbcUrl: String = underlyingUnsafeContainer.getJdbcUrl

  def databaseName: String = underlyingUnsafeContainer.getDatabaseName

  def username: String = underlyingUnsafeContainer.getUsername

  def password: String = underlyingUnsafeContainer.getPassword

  def jdbcDriverInstance: Driver = underlyingUnsafeContainer.getJdbcDriverInstance
}

object JdbcDatabaseContainer {
  case class CommonParams(
                           startupTimeout: FiniteDuration = 120.seconds,
                           connectTimeout: FiniteDuration = 120.seconds,
                           initScriptPath: Option[String] = None
                         ) {
    def applyTo[C <: JavaJdbcDatabaseContainer[_]](container: C): Unit = {
      container.withStartupTimeoutSeconds(startupTimeout.toSeconds.toInt)
      container.withConnectTimeoutSeconds(connectTimeout.toSeconds.toInt)
      initScriptPath.foreach(container.withInitScript)
    }
  }
}

class MySQLContainer(
                      configurationOverride: Option[String] = None,
                      mysqlImageVersion: Option[DockerImageName] = None,
                      databaseName: Option[String] = None,
                      mysqlUsername: Option[String] = None,
                      mysqlPassword: Option[String] = None,
                      urlParams: Map[String, String] = Map.empty,
                      commonJdbcParams: JdbcDatabaseContainer.CommonParams = JdbcDatabaseContainer.CommonParams()
                    ) extends SingleContainer[JavaMySQLContainer[_]] with JdbcDatabaseContainer {

  override val container: JavaMySQLContainer[_] = {
    val c: JavaMySQLContainer[_] = mysqlImageVersion
      .map(new JavaMySQLContainer(_))
      .getOrElse(new JavaMySQLContainer(MySQLContainer.DEFAULT_MYSQL_VERSION))

    databaseName.map(c.withDatabaseName)
    mysqlUsername.map(c.withUsername)
    mysqlPassword.map(c.withPassword)

    configurationOverride.foreach(c.withConfigurationOverride)
    urlParams.foreach { case (key, value) =>
      c.withUrlParam(key, value)
    }

    commonJdbcParams.applyTo(c)

    c
  }

  def testQueryString: String = container.getTestQueryString

}

object MySQLContainer {

  val defaultDockerImageName = DockerImageName.parse(s"${JavaMySQLContainer.IMAGE}:${JavaMySQLContainer.DEFAULT_TAG}")
  val defaultDatabaseName = "test"
  val defaultUsername = "test"
  val defaultPassword = "test"

  val DEFAULT_MYSQL_VERSION = defaultDockerImageName

  def apply(
             configurationOverride: String = null,
             mysqlImageVersion: DockerImageName = null,
             databaseName: String = null,
             username: String = null,
             password: String = null
           ): MySQLContainer = {
    new MySQLContainer(
      Option(configurationOverride),
      Option(mysqlImageVersion),
      Option(databaseName),
      Option(username),
      Option(password)
    )
  }

  case class Def(
                  dockerImageName: DockerImageName = defaultDockerImageName,
                  databaseName: String = defaultDatabaseName,
                  username: String = defaultUsername,
                  password: String = defaultPassword,
                  configurationOverride: Option[String] = None,
                  urlParams: Map[String, String] = Map.empty,
                  commonJdbcParams: JdbcDatabaseContainer.CommonParams = JdbcDatabaseContainer.CommonParams()
                ) extends ContainerDef {

    override type Container = MySQLContainer

    override def createContainer(): MySQLContainer = {
      new MySQLContainer(
        mysqlImageVersion = Some(dockerImageName),
        databaseName = Some(databaseName),
        mysqlUsername = Some(username),
        mysqlPassword = Some(password),
        configurationOverride = configurationOverride,
        urlParams = urlParams,
        commonJdbcParams = commonJdbcParams
      )
    }
  }

}

class HelloSlickTest extends AsyncFlatSpec with ForAllTestContainer {

  val myImage = DockerImageName.parse("0.0.0.0:6000/mysql:5.7.22").asCompatibleSubstituteFor("mysql");

  override val container = MySQLContainer(mysqlImageVersion = myImage)

  "A HelloSlick" should "connect to the Docker mysql instance" in {
    val q = sql"""SHOW VARIABLES LIKE "version";""".as[(String, String)]
    val db: Database =
      connectToDB(s"${container.jdbcUrl}?user=${container.username}&password=${container.password}")

    val f = db.run(q)

    f.map {
      r =>
        assert(r(0)._1 == "version")
    }
  }

}
