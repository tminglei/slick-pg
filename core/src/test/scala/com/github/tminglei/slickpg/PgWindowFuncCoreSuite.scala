package com.github.tminglei.slickpg

import org.scalatest.FunSuite
import slick.ast.Library.SqlFunction
import slick.ast.LiteralNode
import slick.jdbc.JdbcType
import slick.lifted.OptionMapperDSL

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by tminglei on 5/5/16.
  */
class PgWindowFuncCoreSuite extends FunSuite {
  import ExPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class Tab(col1: String, col2: String, col3: String, col4: Int)

  class Tabs(tag: Tag) extends Table[Tab](tag, "TAB_Window_func") {
    def col1 = column[String]("COL1")
    def col2 = column[String]("COL2")
    def col3 = column[String]("COL3")
    def col4 = column[Int]("COL4")

    def * = (col1, col2, col3, col4) <> (Tab.tupled, Tab.unapply)
  }
  val tabs = TableQuery[Tabs]

  ///

  object AggLibrary {
    val Avg = new SqlFunction("avg")
    val StringAgg = new SqlFunction("string_agg")

    val Rank = new SqlFunction("rank")
    val FirstValue = new SqlFunction("first_value")
  }

  def avg[T : JdbcType](c: Rep[T]) = agg.AggFuncRep[T](AggLibrary.Avg, List(c.toNode))
  def stringAgg[P,R](c: Rep[P], delimiter: String)(implicit om: OptionMapperDSL.arg[String,P]#to[String,R]) =
    agg.AggFuncRep[String](AggLibrary.StringAgg, List(c.toNode, LiteralNode(delimiter)))

  def rank() = window.WindowFunc[Long](AggLibrary.Rank, Nil)
  def firstValue[R: JdbcType](c: Rep[R]) = window.WindowFunc[R](AggLibrary.FirstValue, List(c.toNode))

  test("window function core") {
    val sql1 = tabs.map { t =>
      val w = Over.rowsFrame(RowCursor.BoundPreceding(2))
      (stringAgg(t.col1.?, ",").filter(t.col4 < 5) :: w, stringAgg(t.col1.?, ",") :: w, avg(t.col4).over.partitionBy(t.col2))
    }.result.statements.head
    println(s"sql1: $sql1")

    val sql2 = tabs.map { t =>
      val w = Over.partitionBy(t.col2).sortBy(t.col4 desc)
      (rank() :: w, firstValue(t.col1).over.partitionBy(t.col2))
    }.result.statements.head
    println(s"sql2: $sql2")

    Await.result(db.run(
      DBIO.seq(
        (tabs.schema) create,
        tabs ++= Seq(
          Tab("foo", "bar",  "bat", 1),
          Tab("foo", "bar",  "bat", 2),
          Tab("foo", "quux", "bat", 3),
          Tab("baz", "quux", "bat", 4),
          Tab("az", "quux", "bat", 5)
        )
      ).andThen(
        DBIO.seq(
          tabs.map { t =>
            val w = Over.rowsFrame(RowCursor.BoundPreceding(2))
            (stringAgg(t.col1.?, ",").filter(t.col4 < 5) :: w, stringAgg(t.col1.?, ",") :: w, avg(t.col4).over.partitionBy(t.col2))
          }.result.map { r =>
            val expected = List(
              ("foo", "foo", 1),
              ("foo,foo", "foo,foo", 1),
              ("foo,foo,foo", "foo,foo,foo", 4),
              ("foo,foo,baz", "foo,foo,baz", 4),
              ("foo,baz", "foo,baz,az", 4)
            )
            assert(expected === r)
          },
          tabs.map { t =>
            val w = Over.partitionBy(t.col2).sortBy(t.col4 desc)
            (rank() :: w, firstValue(t.col1).over.partitionBy(t.col2))
          }.result.map { r =>
            val expected = List(
              (1, "foo"),
              (2, "foo"),
              (1, "az"),
              (2, "az"),
              (3, "az")
            )
            assert(expected === r)
          }
        )
      ).andFinally(
        (tabs.schema) drop
      ).transactionally
    ), Duration.Inf)
  }
}
