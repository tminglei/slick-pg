package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import scala.util.Try

class PgInheritsTest {
  import ExPostgresDriver.simple._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

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

  @Test
  def testInherits {
    db withSession { implicit session: Session =>
      tabs1 ++= Seq(
        Tab1("foo", "bar",  "bat", 1),
        Tab1("foo", "bar",  "bat", 2),
        Tab1("foo", "quux", "bat", 3),
        Tab1("baz", "quux", "bat", 4)
      )
      tabs2 ++= Seq(
        Tab2("plus", "bar",  "bat", 5, 101),
        Tab2("plus", "quux", "bat", 6, 102)
      )

      ///
      val expected = Seq(
        Tab1("foo", "bar",  "bat", 1),
        Tab1("foo", "bar",  "bat", 2),
        Tab1("foo", "quux", "bat", 3),
        Tab1("baz", "quux", "bat", 4),
        Tab1("plus", "bar",  "bat", 5),
        Tab1("plus", "quux", "bat", 6)
      )
      val q = tabs1.sortBy(_.col4)
      println(s"q = ${q.selectStatement}")
      assertEquals(expected, q.list)

      ///
      val expected1 = Seq(
        Tab2("plus", "bar",  "bat", 5, 101),
        Tab2("plus", "quux", "bat", 6, 102)
      )
      val q1 = tabs2.sortBy(_.col4)
      println(s"q1 = ${q1.selectStatement}")
      assertEquals(expected1, q1.list)

      //
      tabs2.filter(_.col4 === 5.bind).mutate { m =>
        m.row = m.row.copy(col3 = "bat1")
      }
      val q2 = tabs2.filter(_.col4 === 5.bind)
      val expect2 = Tab2("plus", "bar",  "bat1", 5, 101)
      assertEquals(expect2, q2.first)
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      Try { (tabs1.ddl ++ tabs2.ddl).dropStatements.foreach(s => println(s"[inherits] $s")) }
      Try { (tabs1.ddl ++ tabs2.ddl).drop }
      Try { (tabs1.ddl ++ tabs2.ddl).createStatements.foreach(s => println(s"[inherits] $s")) }
      Try { (tabs1.ddl ++ tabs2.ddl).create }
    }
  }
}
