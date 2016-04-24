package com.github.tminglei.slickpg

import org.scalatest.FunSuite
import slick.driver.JdbcProfile
import slick.profile.Capability

import scala.concurrent.Await
import scala.concurrent.duration._

class PgUpsertSuite extends FunSuite {

  object MyPostgresDriver extends ExPostgresDriver {
    // Add back `capabilities.insertOrUpdate` to enable native `upsert` support
    override protected def computeCapabilities: Set[Capability] =
      super.computeCapabilities + JdbcProfile.capabilities.insertOrUpdate
  }

  ///---

  import ExPostgresDriver.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class Bean(id: Long, col1: String, col2: Int)

  class UpsertTestTable(tag: Tag) extends Table[Bean](tag, "test_tab_upsert") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def col1 = column[String]("col1")
    def col2 = column[Int]("col2")

    def * = (id, col1, col2) <> (Bean.tupled, Bean.unapply)
  }
  val UpsertTests = TableQuery[UpsertTestTable]

  //------------------------------------------------------------------------------

  test("emulate upsert support") {

    val upsertSql = ExPostgresDriver.compileInsert(UpsertTests.toNode).upsert.sql
    println(s"upsert sql: $upsertSql")

    assert(upsertSql.contains("begin;"))

    Await.result(db.run(
      DBIO.seq(
        (UpsertTests.schema) create,
        ///
        UpsertTests forceInsertAll Seq(
          Bean(101, "aa", 3),
          Bean(102, "bb", 5),
          Bean(103, "cc", 11)
        ),
        UpsertTests.insertOrUpdate(Bean(101, "a1", 3)),
        UpsertTests.insertOrUpdate(Bean(107, "dd", 7))
      ).andThen(
        DBIO.seq(
          UpsertTests.sortBy(_.id).to[List].result.map(
            r => assert(Seq(
              Bean(1, "dd", 7),
              Bean(101, "a1", 3),
              Bean(102, "bb", 5),
              Bean(103, "cc", 11)
            ) === r)
          )
        )
      ).andFinally(
        (UpsertTests.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("native upsert support") {
    import MyPostgresDriver.api._

    val upsertSql = MyPostgresDriver.compileInsert(UpsertTests.toNode).upsert.sql
    println(s"upsert sql: $upsertSql")

    assert(upsertSql.contains("on conflict"))

    Await.result(db.run(
      DBIO.seq(
        (UpsertTests.schema) create,
        ///
        UpsertTests forceInsertAll Seq(
          Bean(101, "aa", 3),
          Bean(102, "bb", 5),
          Bean(103, "cc", 11)
        ),
        UpsertTests.insertOrUpdate(Bean(101, "a1", 3)),
        UpsertTests.insertOrUpdate(Bean(107, "dd", 7))
      ).andThen(
        DBIO.seq(
          UpsertTests.sortBy(_.id).to[List].result.map(
            r => assert(Seq(
              Bean(101, "a1", 3),
              Bean(102, "bb", 5),
              Bean(103, "cc", 11),
              Bean(107, "dd", 7)
            ) === r)
          )
        )
      ).andFinally(
        (UpsertTests.schema) drop
      ).transactionally
    ), Duration.Inf)
  }
}
