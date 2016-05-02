package com.github.tminglei.slickpg

import org.scalatest.FunSuite
import slick.ast.Library.SqlFunction
import slick.ast.LiteralNode

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PgAggFuncBaseTest extends FunSuite with agg.PgAggFuncBase {
  import ExPostgresDriver.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class Tab(name: String, count: Int, x: Double, y: Double)

  class Tabs(tag: Tag) extends Table[Tab](tag, "test_agg_tab1") {
    def name = column[String]("col2")
    def count = column[Int]("count")
    def x = column[Double]("x")
    def y = column[Double]("y")

    def * = (name, count, x, y) <> (Tab.tupled, Tab.unapply)
  }
  val tabs = TableQuery(new Tabs(_))

  ///

  object AggLibrary {
    val Avg = new SqlFunction("avg")
    val StringAgg = new SqlFunction("string_agg")
    val Corr = new SqlFunction("corr")
  }

  case class Avg[T]() extends AggFuncPartsBase[T, T](AggLibrary.Avg)
  case class StringAgg(delimiter: String) extends AggFuncPartsBase[String, String](AggLibrary.StringAgg, List(LiteralNode(delimiter)))
  case class Corr() extends AggFuncPartsBase[Double, Double](AggLibrary.Corr)

  ///---

  test("agg function base") {
    val sql1 = tabs.map { t => (t.name ^: StringAgg(",").distinct().sortBy(t.name).filter(t.count <= 3), t.count ^: Avg[Int]) }.result.statements.head
    println(s"sql1: $sql1")

    val sql2 = tabs.map { t => (t.y, t.x) ^: Corr() }.result.statements.head
    println(s"sql2: $sql2")

    Await.result(db.run(
      DBIO.seq(
        (tabs.schema).create,
        tabs ++= Seq(
          Tab("foo", 1, 103.05, 179.17),
          Tab("quux", 3, 57.39, 99.07),
          Tab("bar", 2, 35.89, 101.33),
          Tab("bar", 11, 73.75, 28.57)
        )
      ).andThen(
        DBIO.seq(
          tabs.map {
            t => (t.name ^: StringAgg(",").distinct().sortBy(t.name).filter(t.count <= 3), t.count ^: Avg[Int])
          }.result.head.map {
            r => assert(("bar,foo,quux", 4) === r)
          },
          tabs.map {
            t => (t.y, t.x) ^: Corr()
          }.result.head.map {
            r => assert(Math.abs(0.45 - r) < 0.01d)
          }
        )
      ).andFinally(
        (tabs.schema) drop
      ).transactionally
    ), Duration.Inf)
  }
}
