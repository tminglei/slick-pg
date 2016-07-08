package com.github.tminglei.slickpg

import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration._

class PgInheritsSuite extends FunSuite {
  import ExPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  abstract class BaseT[T](tag: Tag, tname: String = "test_tab1") extends Table[T](tag, tname) {
    def col1 = column[String]("COL1")
    def col2 = column[String]("COL2")
    def col3 = column[String]("COL3")
    def col4 = column[Int]("COL4", O.PrimaryKey)
  }

  case class Tab1(col1: String, col2: String, col3: String, col4: Int)

  class Tabs1(tag: Tag) extends BaseT[Tab1](tag, "test_tab1") {
    def * = (col1, col2, col3, col4) <> (Tab1.tupled, Tab1.unapply)
  }
  val tabs1 = TableQuery(new Tabs1(_))

  ///
  case class Tab2(col1: String, col2: String, col3: String, col4: Int, col5: Long)

  class Tabs2(tag: Tag) extends BaseT[Tab2](tag, "test_tab2") with InheritingTable {
    val inherited = tabs1.baseTableRow
    def col5 = column[Long]("col5")

    def * = (col1, col2, col3, col4, col5) <> (Tab2.tupled, Tab2.unapply)
  }
  val tabs2 = TableQuery(new Tabs2(_))

  test("Inherits support") {
    Await.result(db.run(
      DBIO.seq(
        (tabs1.schema ++ tabs2.schema) create,
        ///
        tabs1 ++= Seq(
          Tab1("foo", "bar",  "bat", 1),
          Tab1("foo", "bar",  "bat", 2),
          Tab1("foo", "quux", "bat", 3),
          Tab1("baz", "quux", "bat", 4)
        ),
        tabs2 ++= Seq(
          Tab2("plus", "bar",  "bat", 5, 101),
          Tab2("plus", "quux", "bat", 6, 102)
        )
      ).andThen(
        DBIO.seq(
          tabs1.sortBy(_.col4).to[List].result.map(
            r => assert(Seq(
              Tab1("foo", "bar",  "bat", 1),
              Tab1("foo", "bar",  "bat", 2),
              Tab1("foo", "quux", "bat", 3),
              Tab1("baz", "quux", "bat", 4),
              Tab1("plus", "bar",  "bat", 5),
              Tab1("plus", "quux", "bat", 6)
            ) === r)
          ),
          tabs2.sortBy(_.col4).to[List].result.map(
            r => assert(Seq(
              Tab2("plus", "bar",  "bat", 5, 101),
              Tab2("plus", "quux", "bat", 6, 102)
            ) === r)
          )
        )
      ).andFinally(
        (tabs1.schema ++ tabs2.schema) drop
      ).transactionally
    ), Duration.Inf)
  }
}