import demo.Config
import demo.Tables
import Tables._
import Tables.profile.api._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Example extends App {
  val db = Database.forURL(Config.url, driver=Config.jdbcDriver)

  // Using generated code. Our Build.sbt makes sure they are generated before compilation.
  // TableQuery names are lower case just as we customized them.
  val q = Supplier.join(Coffee).on(_.id === _.supId)
    .map{ case (s,c) => (s.name, c.name) }

  println(">>> running Example ...")

  Await.ready(
    db.run {
      q.result.map(
        _.groupBy{ case (s,c) => s }
          .mapValues(_.map{ case (s,c) => c })
          .mkString("\n")
      ).map(println)
    },
    20.seconds
  )
}