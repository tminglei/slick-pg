package com.github.tminglei.slickpg
package trgm

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by minglei on 6/21/17.
  */
class PgTrgmSupportSuite extends AnyFunSuite with PostgresContainer {

  trait MyPostgresProfile1 extends ExPostgresProfile with PgTrgmSupport {
    override val api: MyAPI = new MyAPI {}

    ///
    trait MyAPI extends ExtPostgresAPI with PgTrgmImplicits
  }
  object MyPostgresProfile1 extends MyPostgresProfile1

  ///
  import MyPostgresProfile1.api._

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  case class StrBean(id: Long, str: String)

  class StringTestTable(tag: Tag) extends Table[StrBean](tag, "trgm_test") {
    val id = column[Long]("id")
    val str = column[String]("str")

    def * = (id, str) <> (StrBean.tupled, StrBean.unapply)
  }
  val trgmTestTable = TableQuery[StringTestTable]

  ///
  val testRec1 = StrBean(101L, "hello")
  val testRec2 = StrBean(102L, "see you")
  val testRec3 = StrBean(103L, "how are you")

  test("String Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        trgmTestTable.schema create,
        trgmTestTable forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          trgmTestTable.filter(_.id === 101L).map(_.str % "hi").result.head.map {
            r => assert(false === r)
          },
          trgmTestTable.filter(_.id === 101L).map(_.str `<%` "hello").result.head.map {
            r => assert(true === r)
          },
          trgmTestTable.filter(_.id === 101L).map(_.str %> "hello").result.head.map {
            r => assert(true === r)
          },
          trgmTestTable.filter(_.id === 101L).map(_.str <<% "hello").result.head.map {
            r => assert(true === r)
          },
          trgmTestTable.filter(_.id === 101L).map(_.str %>> "hello").result.head.map {
            r => assert(true === r)
          },
          trgmTestTable.filter(_.id === 102L).map(_.str <-> "hi").result.head.map {
            r => assert(Math.abs(r - 1.0d) < 0.1d)
          },
          trgmTestTable.filter(_.id === 103L).map(_.str <<-> "hi").result.head.map {
            r => assert(r < 1.0d)
          },
          trgmTestTable.filter(_.id === 102L).map(_.str <->> "hi").result.head.map {
            r => assert(Math.abs(r - 1.0d) < 0.1d)
          },
          trgmTestTable.filter(_.id === 103L).map(_.str <<<-> "hi").result.head.map {
            r => assert(r < 1.0d)
          },
          trgmTestTable.filter(_.id === 102L).map(_.str <->>> "hi").result.head.map {
            r => assert(Math.abs(r - 1.0d) < 0.1d)
          },
          trgmTestTable.filter(_.id === 103L).map(_.str.similarity("hi")).result.head.map {
            r => assert(r < 1.0d)
          },
          trgmTestTable.filter(_.id === 103L).map(_.str.wordSimilarity("hi")).result.head.map {
            r => assert(r < 1.0d)
          },
          trgmTestTable.filter(_.id === 103L).map(_.str.strictWordSimilarity("hi")).result.head.map {
            r => assert(r < 1.0d)
          }
        )
      ).andFinally(
        trgmTestTable.schema drop
      )
    ), Duration.Inf)
  }
}
