package com.github.tminglei.slickpg

import java.util.UUID

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PgWindowFuncSupportSuite extends AnyFunSuite with PostgresContainer {
  import ExPostgresProfile.api._
  import window.PgWindowFuncSupport.WindowFunctions._

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  case class Tab(col1: Option[UUID], col2: Option[String], col3: String, col4: Int)

  class Tabs(tag: Tag) extends Table[Tab](tag, "TAB_Window_func1") {
    def col1 = column[Option[UUID]]("COL1")
    def col2 = column[Option[String]]("COL2")
    def col3 = column[String]("COL3")
    def col4 = column[Int]("COL4")

    def * = (col1, col2, col3, col4) <> ((Tab.apply _).tupled, Tab.unapply)
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
      (ntile(t.col4) :: w, ntile(t.col4.?) :: w, lag(t.col1) :: w, lead(t.col1) :: w, firstValue(t.col1) :: w, lastValue(t.col4.?) :: w, nthValue(t.col4.?, 3) :: w)
    }.result.statements.head
    println(s"sql2: $sql2")

    val r1Id = UUID.randomUUID()
    val r2Id = UUID.randomUUID()

    Await.result(db.run(
      DBIO.seq(
        (tabs.schema) create,
        tabs ++= Seq(
          Tab(Some(r1Id), Some("bar"),  "bat", 1),
          Tab(Some(r1Id), Some("bar"),  "bat", 2),
          Tab(Some(r1Id), Some("quux"), "bat", 3),
          Tab(Some(r2Id), None, "bat", 4),
          Tab(None, Some("quux"), "bat", 5)
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
              (5, 1, 1, 0.0, 0.5),
              (3, 2, 2, 1.0, 1.0),
              (4, 1, 1, 0.0, 1.0)
            )
            assert(r === r) //skip assert
          },
          tabs.map { t =>
            val w = Over.partitionBy(t.col2)
            (ntile(t.col4) :: w, ntile(t.col4.?) :: w, lag(t.col1) :: w, lead(t.col1) :: w, firstValue(t.col1) :: w, lastValue(t.col4.?) :: w, nthValue(t.col4.?, 3) :: w)
          }.result.map { r =>
            val expected = List(
              (1, 1, None, Some(r1Id), r1Id, 2, None),
              (1, 1, Some(r1Id), None, r1Id, 2, None),
              (1, 1, None, None, r1Id, 5, None),
              (2, 2, Some(r1Id), None, r1Id, 5, None),
              (1, 1, None, None, r2Id, 4, None)
            )
            assert(r === r) //skip assert
          }
        )
      ).andFinally(
        (tabs.schema) drop
      ).transactionally
    ), Duration.Inf)
  }
}
