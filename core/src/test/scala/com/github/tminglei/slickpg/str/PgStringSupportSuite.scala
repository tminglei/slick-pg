package com.github.tminglei.slickpg
package str

import java.nio.charset.StandardCharsets
import java.util.Base64

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by minglei on 12/12/16.
  */
class PgStringSupportSuite extends AnyFunSuite with PostgresContainer {

  trait MyPostgresProfile1 extends ExPostgresProfile with PgStringSupport {
    override val api: API = new API {}

    ///
    trait API extends super.API with PgStringImplicits
  }
  object MyPostgresProfile1 extends MyPostgresProfile1

  ///
  import MyPostgresProfile1.api._

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  case class StrBean(id: Long, str: String, strArr: Array[Byte])

  class StringTestTable(tag: Tag) extends Table[StrBean](tag, "string_test") {
    val id = column[Long]("id")
    val str = column[String]("str")
    val strArr = column[Array[Byte]]("str_arr")

    def * = (id, str, strArr) <> (StrBean.tupled, StrBean.unapply)
  }
  val stringTestTable = TableQuery[StringTestTable]

  ///
  val base64_arr = Base64.getEncoder.encode("test3".getBytes)

  val testRec1 = StrBean(101L, "test1", "test1".getBytes(StandardCharsets.UTF_8))
  val testRec2 = StrBean(102L, "test2", "test2".getBytes(StandardCharsets.US_ASCII))
  val testRec3 = StrBean(103L, "ewAB", Array("123".toByte, "000".toByte, "001".toByte))

  test("String Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        stringTestTable.schema create,
        stringTestTable forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          stringTestTable.filter(_.str ilike "Test%").map(_.id).to[List].result.map {
            r => assert(List(testRec1.id, testRec2.id) === r)
          },
          stringTestTable.filter(_.str ~ "test.*").map(_.id).to[List].result.map {
            r => assert(List(testRec1.id, testRec2.id) === r)
          },
          stringTestTable.filter(_.str ~* "TEST.*").map(_.id).to[List].result.map {
            r => assert(List(testRec1.id, testRec2.id) === r)
          },
          stringTestTable.filter(_.str !~ "ew.*").map(_.id).to[List].result.map {
            r => assert(List(testRec1.id, testRec2.id) === r)
          },
          stringTestTable.filter(_.str !~* "EW.*").map(_.id).to[List].result.map {
            r => assert(List(testRec1.id, testRec2.id) === r)
          },
          stringTestTable.filter(_.id === 101L).map(_.strArr.convert("utf-8", "sql_ascii")).result.head.map {
            r => assert("test1" === new String(r, StandardCharsets.US_ASCII))
          },
          stringTestTable.filter(_.id === 101L).map(r => r.str.convertTo("utf-8") === r.strArr).result.head.map {
            r => assert(true === r)
          },
          stringTestTable.filter(_.id === 102L).map(r => r.strArr.convertFrom("sql_ascii")).result.head.map {
            r => assert(testRec2.str === r)
          },
          stringTestTable.filter(_.id === 103L).map(r => r.str.decode("base64") === r.strArr).result.head.map {
            r => assert(true === r)
          },
          stringTestTable.filter(_.id === 103L).map(r => r.strArr.encode("base64")).result.head.map {
            r => assert(testRec3.str === r)
          }
        )
      ).andFinally(
        stringTestTable.schema drop
      )
    ), Duration.Inf)
  }
}
