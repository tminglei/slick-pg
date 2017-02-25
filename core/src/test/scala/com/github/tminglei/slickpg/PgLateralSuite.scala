package com.github.tminglei.slickpg

import org.scalatest.FunSuite
import slick.ast.JoinType

import scala.concurrent.Await
import scala.concurrent.duration._

class PgLateralSuite extends FunSuite {
  import ExPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class Bean(id: Long, col1: String, col2: String)

  class LateralTestTable(tag: Tag) extends Table[Bean](tag, "test_tab_lateral") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def col1 = column[String]("col1")
    def col2 = column[String]("col2")

    def * = (id, col1, col2) <> (Bean.tupled, Bean.unapply)
  }
  val LateralTests = TableQuery[LateralTestTable]

  //--------------------------------------------------------------------------

  test("lateral join") {
    val lateralSql = LateralTests.filter(_.col2 === "home").lateral(l1 =>
      LateralTests.filter(l2 => l2.col1 === l1.col1 && l2.col2 === "census"), jt = JoinType.Inner
    ).map { case (l1, l2) => (l1.col1, l1.col2, l2.col2) }.result.statements.head

    println(s"lateralSql: $lateralSql")

    Await.result(db.run(
      DBIO.seq(
        (LateralTests.schema) create,
        ///
        LateralTests forceInsertAll Seq(
          Bean(101, "aa", "home"),
          Bean(102, "bb", "home"),
          Bean(103, "cc", "home"),
          Bean(104, "dd", "census"),
          Bean(105, "aa", "census"),
          Bean(106, "bb", "census"),
          Bean(107, "aa", "quote"),
          Bean(108, "aa", "register")
        )
      ).andThen(
        DBIO.seq(
          LateralTests.filter(_.col2 === "home").lateral(l1 =>
            LateralTests.filter(l2 => l2.col1 === l1.col1 && l2.col2 === "census"), jt = JoinType.Inner
          ).map { case (l1, l2) => (l1.col1, l1.col2, l2.col2) }.to[List].result.map {
            r => assert(Seq(("aa", "home", "census"), ("bb", "home", "census")) === r)
          }
        )
      ).andFinally(
        (LateralTests.schema) drop
      ).transactionally
    ), Duration.Inf)

  }
}
