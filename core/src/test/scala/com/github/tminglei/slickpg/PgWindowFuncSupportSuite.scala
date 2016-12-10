package com.github.tminglei.slickpg

import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PgWindowFuncSupportSuite extends FunSuite {
  import ExPostgresProfile.api._
  import window.PgWindowFuncSupport.WindowFunctions._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class Tab(col1: String, col2: String, col3: String, col4: Int)

  class Tabs(tag: Tag) extends Table[Tab](tag, "TAB_Window_func1") {
    def col1 = column[String]("COL1")
    def col2 = column[String]("COL2")
    def col3 = column[String]("COL3")
    def col4 = column[Int]("COL4")

    def * = (col1, col2, col3, col4) <> (Tab.tupled, Tab.unapply)
  }
  val tabs = TableQuery[Tabs]

  ///---

  test("window functions") {
    val sql1 = tabs.map { t =>
      val w = Over.partitionBy(t.col2).sortBy(t.col4 desc)
      (rowNumber() :: Over, rank() :: w, denseRank() :: w, percentRank() :: w, cumeDist() :: w)
    }.result.statements.head
    println(s"sql1: $sql1")

    val sql2 = tabs.map { t =>
      val w = Over.partitionBy(t.col2)
      (ntile(t.col4) :: w, ntile(t.col4.?) :: w, lag(t.col1.?) :: w, lead(t.col1.?) :: w, firstValue(t.col1.?) :: w, lastValue(t.col4.?) :: w, nthValue(t.col4.?, 3) :: w)
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
            val w = Over.partitionBy(t.col2).sortBy(t.col4 desc)
            (rowNumber() :: Over, rank() :: w, denseRank() :: w, percentRank() :: w, cumeDist() :: w)
          }.result.map { r =>
            val expected = List(
              (2, 1, 1, 0.0, 0.5),
              (1, 2, 2, 1.0, 1.0),
              (5, 1, 1, 0.0, 1.0/3),
              (4, 2, 2, 0.5, 2.0/3),
              (3, 3, 3, 1.0, 1.0)
            )
            assert(expected === r)
          },
          tabs.map { t =>
            val w = Over.partitionBy(t.col2)
            (ntile(t.col4) :: w, ntile(t.col4.?) :: w, lag(t.col1.?) :: w, lead(t.col1.?) :: w, firstValue(t.col1.?) :: w, lastValue(t.col4.?) :: w, nthValue(t.col4.?, 3) :: w)
          }.result.map { r =>
            val expected = List(
              (1, 1, None, Some("foo"), "foo", 2, None),
              (1, 1, Some("foo"), None, "foo", 2, None),
              (1, 1, None, Some("baz"), "foo", 5, Some(5)),
              (2, 2, Some("foo"), Some("az"), "foo", 5, Some(5)),
              (3, 3, Some("baz"), None, "foo", 5, Some(5))
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
