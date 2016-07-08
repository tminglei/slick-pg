package com.github.tminglei.slickpg

import org.scalatest.FunSuite
import slick.ast.Library.SqlFunction
import slick.ast.{LiteralNode, ScalaBaseType}
import slick.jdbc.JdbcType
import slick.lifted.OptionMapperDSL

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PgAggFuncCoreSuite extends FunSuite {
  import ExPostgresProfile.api._

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

    val PercentileDisc = new SqlFunction("percentile_disc")
    val PercentRank = new SqlFunction("percent_rank")
  }

  def avg[T : JdbcType](c: Rep[T]) = agg.AggFuncRep[T](AggLibrary.Avg, List(c.toNode))
  def stringAgg[P,R](c: Rep[P], delimiter: String)(implicit om: OptionMapperDSL.arg[String,P]#to[String,R]) =
    agg.AggFuncRep[String](AggLibrary.StringAgg, List(c.toNode, LiteralNode(delimiter)))
  def corr[P1,P2,R](c1: Rep[P1], c2: Rep[P2])(implicit om: OptionMapperDSL.arg[Double,P1]#arg[Double,P2]#to[Double,R]) =
    agg.AggFuncRep[Double](AggLibrary.Corr, List(c1.toNode, c2.toNode))

  def percentileDisc(f: Double) = agg.OrderedAggFuncRep(AggLibrary.PercentileDisc, List(LiteralNode(f)))
  def percentRank[T: ScalaBaseType](v: T) = agg.OrderedAggFuncRep.withTypes[Any,Double](AggLibrary.PercentRank, List(LiteralNode(v)))

  ///---

  test("agg function core") {
    val sql1 = tabs.map { t =>
      (stringAgg(t.name.?, ",").sortBy(t.name).distinct().filter(t.count <= 3), avg(t.count).filter(t.count < 10))
    }.result.statements.head
    println(s"sql1: $sql1")

    val sql2 = tabs.map { t =>
      (corr(t.y, t.x.?), percentileDisc(0.5d).filter(t.y < 130d).within(t.x desc), percentRank("bar").within(t.name))
    }.result.statements.head
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
          tabs.map { t =>
            (stringAgg(t.name.?, ",").distinct().sortBy(t.name).filter(t.count <= 3), avg(t.count).filter(t.count < 10))
          }.result.head.map { r =>
            assert(("bar,foo,quux", 2) === r)
          },
          tabs.map { t =>
            (corr(t.y, t.x.?), percentileDisc(0.5d).filter(t.y < 130d).within(t.x desc), percentRank("bar").within(t.name))
          }.result.head.map { case (corr, percentileDisc, percentRank) =>
            assert(Math.abs(0.447d - corr) < 0.01d)
            assert(Math.abs(57.39d - percentileDisc) < 0.01d)
            assert(Math.abs(percentRank) < 0.01d)
          }
        )
      ).andFinally(
        (tabs.schema) drop
      ).transactionally
    ), Duration.Inf)
  }
}
