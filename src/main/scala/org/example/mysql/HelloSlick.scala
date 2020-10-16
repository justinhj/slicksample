package org.example.mysql

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.slf4j.LoggerFactory
import slick.jdbc.MySQLProfile.api._
import slick.util.AsyncExecutor

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object HelloSlick extends App {

  val log = LoggerFactory.getLogger("HelloSlick")

  def connectToDB(connectionUrl: String): Database = {
    val modifiedURl = connectionUrl + "&characterEncoding=UTF-8&useServerPrepStmts=false&rewriteBatchedStatements=true"
    val maxPoolSize = 10

    // Configure the HikariCP for this database
    val hikariConfig = new HikariConfig()
    hikariConfig.setPoolName("test1")
    hikariConfig.setJdbcUrl(modifiedURl)
    hikariConfig.setConnectionTimeout(5000)
    hikariConfig.setIdleTimeout(100000)
    hikariConfig.setMaxLifetime(1000000)
    hikariConfig.setMaximumPoolSize(maxPoolSize)
    hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver")

    val dsTry: Try[HikariDataSource] = Try(new HikariDataSource(hikariConfig))

    dsTry match {
      case Success(ds) => {
        val queueSize = 1000
        val executorName = s"AsyncDbExecutor-test1"
        val asyncExecutor = AsyncExecutor(executorName, maxPoolSize, queueSize)
        Database.forDataSource(ds, Some(maxPoolSize), asyncExecutor)
      }
      case Failure(err) =>
        log.error(err.getMessage())
        throw err;
    }
  }

  val q = sql"""SHOW VARIABLES LIKE "version";""".as[(String, String)]

  val db: Database = connectToDB("jdbc:mysql://127.0.0.1/?user=yay&password=yay")

  val r = db.run(q)

  val result = Await.result(r, 10 seconds)

  log.info(result.toString)

}
