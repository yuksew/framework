package slicksupport

import org.scalatra.ScalatraServlet
import org.scalatra._
import scalate.ScalateSupport
import com.mchange.v2.c3p0.ComboPooledDataSource
import java.util.Properties
import org.slf4j.LoggerFactory

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

// Definition of the SUPPLIERS table
case class Task(id: Option[Int], body: String, isCompleted: Boolean)
object Tasks extends Table[Task]("tasks") {
  def id = column[Int]("task_id", O.PrimaryKey, O.AutoInc) // This is the primary key column
  def body = column[String]("body")
  def isCompleted = column[Boolean]("isCompleted")

  // Every table needs a * projection with the same type as the table's type parameter
  def * = id.? ~ body ~ isCompleted <> (Task, Task.unapply _)
}

trait SlickSupport extends ScalatraServlet {

  val logger = LoggerFactory.getLogger(getClass)

  val cpds = {
    val props = new Properties
    props.load(getClass.getResourceAsStream("/c3p0.properties"))
    val cpds = new ComboPooledDataSource
    cpds.setProperties(props)
    logger.info("Created c3p0 connection pool")
    cpds
  }

  def closeDbConnection() {
    logger.info("Closing c3po connection pool")
    cpds.close
  }

  val db = Database.forDataSource(cpds)

  override def destroy() {
    super.destroy()
    closeDbConnection
  }
}

class SlickRoutes extends ScalatraServlet with ScalateSupport with SlickSupport {

  get("/db/create-tables") {
    db withSession {
      (Tasks.ddl).create
    }
  }

  get("/db/load-data") {
    db withSession {

      // Insert some Tasks
      Tasks.insertAll(
        (Task(None, "unCompleted", false)),
        (Task(None, "completed", true))
      )
    }
  }

  get("/db/drop-tables") {
    db withSession {
      (Tasks.ddl).drop
    }
  }

  get("/") {
    db withSession {
      val q3 = for {
        t <- Tasks if t.isCompleted === false
      } yield (t)
      contentType = "text/html"
      println(q3)
      ssp("index", "tasks" -> q3.list)
    }
  }

  get("/task/view/:id") {
    db withSession {
      val q3 = for {
        t <- Tasks if t.id === params("id").toInt
      } yield (t)
      contentType = "text/html"
      if (q3.list.isEmpty) {
        print("empty set")
      } else {
        ssp("view", "tasks" -> q3.list)
      }
    }
  }
  
  post("/task/add") {
    db withSession {
      Tasks.insert(Task(None, params("body"), false))
      val q3 = for {
        t <- Tasks if t.isCompleted === false
      } yield (t)
      contentType = "text/html"
      ssp("index", "tasks" -> q3.list)
    }
  }
  
  post("/task/edit") {
    db withSession {
      var isCompleted = false
      if (params("isCompleted") == "1" ) {
        isCompleted = true
      }
      Tasks.where(_.id === params("id").toInt).map{ r => r.body ~ r.isCompleted }.update(params("body"), isCompleted)
      val q3 = for {
        t <- Tasks if t.isCompleted === false
      } yield (t)
      contentType = "text/html"
      ssp("index", "tasks" -> q3.list)
    }
  }
}

