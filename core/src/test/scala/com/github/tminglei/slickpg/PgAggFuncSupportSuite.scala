package com.github.tminglei.slickpg

import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PgAggFuncSupportSuite extends FunSuite {
  import ExPostgresProfile.api._

  val PgArrayJdbcTypes = new array.PgArrayJdbcTypes with ExPostgresProfile {}
  implicit val simpleIntListTypeMapper = new PgArrayJdbcTypes.SimpleArrayJdbcType[Int]("int4").to(_.toList)
  implicit val simpleStrListTypeMapper = new PgArrayJdbcTypes.SimpleArrayJdbcType[String]("text").to(_.toList)
  implicit val simpleDoubleListTypeMapper = new PgArrayJdbcTypes.SimpleArrayJdbcType[Double]("float8").to(_.toList)

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class Tab(name: String, count: Int, bool: Boolean, x: Double, y: Double)

  class Tabs(tag: Tag) extends Table[Tab](tag, "test_agg_tab1") {
    def name = column[String]("col2")
    def count = column[Int]("count")
    def bool = column[Boolean]("bool")
    def x = column[Double]("x")
    def y = column[Double]("y")

    def * = (name, count, bool, x, y) <> (Tab.tupled, Tab.unapply)
  }
  val tabs = TableQuery(new Tabs(_))

  val inserts = tabs ++= Seq(
    Tab("foo", 1, true, 103.05, 179.17),
    Tab("quux", 3, true, 57.39, 99.07),
    Tab("bar", 2, false, 35.89, 101.33),
    Tab("bar", 11, true, 73.75, 28.57)
  )

  ///---

  test("general agg functions") {
    import agg.PgAggFuncSupport.GeneralAggFunctions._

    val sql1 = tabs.map { t =>
      (arrayAgg(t.name.?).sortBy(t.x), stringAgg(t.name.?, ",").sortBy(t.name).distinct(), avg(t.count.?).filter(t.count < 10))
    }.result.statements.head
    println(s"sql1: $sql1")

    val sql2 = tabs.map { t =>
      (bitAnd(t.count.?), bitOr(t.count.?).filter(t.count < 10), boolAnd(t.bool.?).sortBy(t.name), boolOr(t.bool))
    }.result.statements.head
    println(s"sql2: $sql2")

    val sql3 = tabs.map { t =>
      (count_*(), count(t.name.?).distinct(), every(t.bool.?).sortBy(t.name), max(t.x.?), min(t.y.?), sum(t.count.?).filter(t.count < 10))
    }.result.statements.head
    println(s"sql3: $sql3")

    Await.result(db.run(
      DBIO.seq(
        (tabs.schema).create,
        inserts
      ).andThen(
        DBIO.seq(
          tabs.map { t =>
            (arrayAgg(t.name.?).sortBy(t.x), stringAgg(t.name.?, ",").sortBy(t.name).distinct(), avg(t.count.?).filter(t.count < 10))
          }.result.head.map { r =>
            assert((List("bar","quux","bar","foo"), "bar,foo,quux", 2) === r)
          },
          tabs.map { t =>
            (bitAnd(t.count.?), bitOr(t.count.?).filter(t.count < 10), boolAnd(t.bool.?).sortBy(t.name), boolOr(t.bool))
          }.result.head.map { r =>
            assert((0, 3, false, true) === r)
          },
          tabs.map { t =>
            (count_*(), count(t.name.?).distinct(), every(t.bool.?).sortBy(t.name), max(t.x.?), min(t.y.?), sum(t.count.?).filter(t.count < 10))
          }.result.head.map { r =>
            assert((4, 3, false, 103.05, 28.57, 6) === r)
          }
        )
      ).andFinally(
        (tabs.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("statistics agg functions") {
    import agg.PgAggFuncSupport.StatisticsAggFunctions._

    val sql1 = tabs.map { t =>
      (corr(t.x, t.y.?), covarPop(t.x, t.y.?).sortBy(t.name), covarSamp(t.x, t.y.?), regrAvgX(t.x, t.y.?).distinct(), regrAvgY(t.x, t.y.?), regrCount(t.x, t.y.?).filter(t.count < 10))
    }.result.statements.head
    println(s"sql1: $sql1")

    val sql2 = tabs.map { t =>
      (regrIntercept(t.x, t.y.?), regrR2(t.x, t.y.?).sortBy(t.name), regrSlope(t.x, t.y.?).distinct(), regrSxx(t.x, t.y.?), regrSxy(t.x, t.y.?).filter(t.count < 10), regrSyy(t.x, t.y.?))
    }.result.statements.head
    println(s"sql2: $sql2")

    val sql3 = tabs.map { t =>
      (stdDev(t.x.?), stdDevPop(t.y.?).sortBy(t.name), stdDevSamp(t.x.?).distinct(), variance(t.y.?), varPop(t.x.?).filter(t.count < 10), varSamp(t.y.?))
    }.result.statements.head
    println(s"sql2: $sql3")

    Await.result(db.run(
      DBIO.seq(
        (tabs.schema).create,
        inserts
      ).andThen(
        DBIO.seq(
          tabs.map { t =>
            (corr(t.x, t.y.?), covarPop(t.x, t.y.?).sortBy(t.name), covarSamp(t.x, t.y.?), regrAvgX(t.x, t.y.?).distinct(), regrAvgY(t.x, t.y.?), regrCount(t.x, t.y.?).filter(t.count < 10))
          }.result.head.map { case (corr, covarPop, covarSamp, regrAvgx, regrAvgy, regrCount) =>
            assert(Math.abs(0.447 - corr) < 0.01d)
            assert(Math.abs(583.814 - covarPop) < 0.01d)
            assert(Math.abs(778.418 - covarSamp) < 0.01d)
            assert(Math.abs(102.035 - regrAvgx) < 0.01d)
            assert(Math.abs(67.52 - regrAvgy) < 0.01d)
            assert(3 === regrCount)
          },
          tabs.map { t =>
            (regrIntercept(t.x, t.y.?), regrR2(t.x, t.y.?).sortBy(t.name), regrSlope(t.x, t.y.?).distinct(), regrSxx(t.x, t.y.?), regrSxy(t.x, t.y.?).filter(t.count < 10), regrSyy(t.x, t.y.?))
          }.result.head.map { case (intercept, r2, slope, sxx, sxy, syy) =>
            assert(Math.abs(46.538 - intercept) < 0.01d)
            assert(Math.abs(0.2 - r2) < 0.01d)
            assert(Math.abs(0.205 - slope) < 0.01d)
            assert(Math.abs(11356.203 - sxx) < 0.01d)
            assert(Math.abs(2945.503 - sxy) < 0.01d)
            assert(Math.abs(2404.267 - syy) < 0.01d)
          },
          tabs.map { t =>
            (stdDev(t.x.?), stdDevPop(t.y.?).sortBy(t.name), stdDevSamp(t.x.?).distinct(), variance(t.y.?), varPop(t.x.?).filter(t.count < 10), varSamp(t.y.?))
          }.result.head.map { case (dev, devpop, devsamp, variance, varpop, varsamp) =>
            assert(Math.abs(28.309 - dev) < 0.01d)
            assert(Math.abs(53.283 - devpop) < 0.01d)
            assert(Math.abs(28.309 - devsamp) < 0.01d)
            assert(Math.abs(3785.40 - variance) < 0.01d)
            assert(Math.abs(784.172 - varpop) < 0.01d)
            assert(Math.abs(3785.40 - varsamp) < 0.01d)
          }
        )
      ).andFinally(
        (tabs.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("ordered-set agg functions") {
    import agg.PgAggFuncSupport.OrderedSetAggFunctions._

    val sql1 = tabs.map { t =>
      (mode().within(t.x), percentileCont(0.5).within(t.x), percentileCont(List(0.45, 0.5)).within(t.x), percentileDisc(0.5).within(t.count), percentileDisc[String](List(0.5,0.3)).within(t.name))
    }.result.statements.head
    println(s"sql1: $sql1")

    Await.result(db.run(
      DBIO.seq(
        (tabs.schema).create,
        inserts
      ).andThen(
        DBIO.seq(
          tabs.map { t =>
            (mode().within(t.x), percentileCont(0.5).within(t.x), percentileCont(List(0.45, 0.5)).within(t.x), percentileDisc(0.5).within(t.count), percentileDisc[String](List(0.5,0.3)).within(t.name))
          }.result.head.map { r =>
            assert((35.89, 65.57, List(63.116, 65.57), 2, List("bar", "bar")) === r)
          }
        )
      ).andFinally(
        (tabs.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("hypothetical-set agg functions") {
    import agg.PgAggFuncSupport.HypotheticalSetAggFunctions._

    val sql1 = tabs.map { t =>
      (rank("bar").within(t.name), denseRank("bar").within(t.name), percentRank("bar").within(t.name), cumeDist("bar").within(t.name))
    }.result.statements.head
    println(s"sql1: $sql1")

    Await.result(db.run(
      DBIO.seq(
        (tabs.schema).create,
        inserts
      ).andThen(
        DBIO.seq(
          tabs.map { t =>
            (rank("bar").within(t.name), denseRank("bar").within(t.name), percentRank("bar").within(t.name), cumeDist("bar").within(t.name))
          }.result.map { r =>
            assert(List((1, 1, 0.0, 0.6)) === r)
          }
        )
      ).andFinally(
        (tabs.schema) drop
      ).transactionally
    ), Duration.Inf)
  }
}
