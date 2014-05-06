package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._

class PgAggFunctionTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class Tab(name: String, count: Int, x: Double, y: Double)

  class Tabs(tag: Tag) extends Table[Tab](tag, "test1_tab1") {
    def name = column[String]("COL2")
    def count = column[Int]("count")
    def x = column[Double]("x")
    def y = column[Double]("y")

    def * = (name, count, x, y) <> (Tab.tupled, Tab.unapply)
  }
  val tabs = TableQuery(new Tabs(_))

  @Test
  def testAggFunctions(): Unit = {
    db withSession { implicit session: Session =>
      import PgAggregateFunctions._

      ///
      tabs ++= Seq(
        Tab("foo", 1, 103.05, 179.17),
        Tab("quux", 3, 57.39, 99.07),
        Tab("bar", 2, 35.89, 101.33),
        Tab("bar", 11, 73.75, 28.57)
      )

//      val q = tabs.map(t => (t.name ^: StringAgg(",").forDistinct().orderBy(t.name), t.count ^: Avg[Int]))
//      println(s"q = ${q.selectStatement}")
//      assertEquals(("bar,foo,quux", 4), q.first)
//
//      val q1 = tabs.map(t => (t.y, t.x) ^: Corr())
//      println(s"q = ${q1.selectStatement}")
//      assertEquals(0.45, q1.first, 0.01)
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      (tabs.ddl).create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      (tabs.ddl).drop
    }
  }
}
