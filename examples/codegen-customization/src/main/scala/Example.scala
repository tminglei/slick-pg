import demo.Config
import demo.Tables
import Tables._
import Tables.profile.simple._

object Example extends App {
  val db = Database.forURL(Config.url, driver=Config.jdbcDriver)

  // Using generated code. Our Build.sbt makes sure they are generated before compilation.
  // TableQuery names are lower case just as we customized them.
  val q = Supplier.join(Coffee).on(_.id === _.supId)
                   .map{ case (s,c) => (s.name, c.name) }

  db.withSession { implicit session =>
    println( q.run.groupBy{ case (s,c) => s }
                  .mapValues(_.map{ case (s,c) => c })
                  .mkString("\n")
    )
  }
}
