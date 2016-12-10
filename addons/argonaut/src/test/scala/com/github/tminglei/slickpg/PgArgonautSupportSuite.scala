package com.github.tminglei.slickpg

import java.util.concurrent.Executors

import argonaut.Argonaut._
import argonaut._
import org.scalatest.FunSuite
import slick.jdbc.{GetResult, PostgresProfile}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class PgArgonautSupportSuite extends FunSuite {
  implicit val testExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  object MyPostgresProfile extends PostgresProfile
                            with PgArgonautSupport
                            with array.PgArrayJdbcTypes {
    override val pgjson = "jsonb"

    override val api = new API with JsonImplicits {
      implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    }

    val plainAPI = new API with ArgonautJsonPlainImplicits
  }

  ///
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: Json)

  class JsonTestTable(tag: Tag) extends Table[JsonBean](tag, "JsonTest4") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[Json]("json")

    def * = (id, json) <> (JsonBean.tupled, JsonBean.unapply)
  }
  val JsonTests = TableQuery[JsonTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = JsonBean(33L, """ { "a":101, "b":"aaa", "c":[3,4,5,9] } """.parseOption.getOrElse(jNull))
  val testRec2 = JsonBean(35L, """ [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """.parseOption.getOrElse(jNull))
  val testRec3 = JsonBean(37L, """ ["a", "b"] """.parseOption.getOrElse(jNull))

  test("Argonaut json Lifted support") {
    val json1 = """ {"a":"v1","b":2} """.parseOption.getOrElse(jNull)
    val json2 = """ {"a":"v5","b":3} """.parseOption.getOrElse(jNull)

    Await.result(db.run(
      DBIO.seq(
        JsonTests.schema create,
        ///
        JsonTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          JsonTests.filter(_.id === testRec2.id.bind).map(_.json).result.head.map(
            r => assert(jArray(List(json1,json2)) === r)
          ),
          // ->>/->
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>>("c")).result.head.map(
            r => assert("[3,4,5,9]" === r.replace(" ", ""))
          ),
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c")).result.head.map(
            r => assert(jArray(List(jNumber(3), jNumber(4), jNumber(5), jNumber(9))) === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1)).result.head.map(
            r => assert(json2 === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1)).result.head.map(
            r => assert("""{"a":"v5","b":3}""" === r.replace(" ", ""))
          ),
          // #>>/#>
          JsonTests.filter(_.id === testRec1.id).map(_.json.#>(List("c"))).result.head.map(
            r => assert(jArray(List(jNumber(3), jNumber(4), jNumber(5), jNumber(9))) === r)
          ),
          JsonTests.filter(_.json.#>>(List("a")) === "101").result.head.map(
            r => assert(testRec1 === r)
          ),
          // {}_array_length
          JsonTests.filter(_.id === testRec2.id).map(_.json.arrayLength).result.head.map(
            r => assert(2 === r)
          ),
          // {}_array_elements
          JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements).to[List].result.map(
            r => assert(List(json1, json2) === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements).result.head.map(
            r => assert(json1 === r)
          ),
          // {}_array_elements_text
          JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElementsText).result.head.map(
            r => assert(json1.toString.replace(" ", "") === r.replace(" ", ""))
          ),
          // {}_object_keys
          JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys).to[List].result.map(
            r => assert(List("a","b","c") === r)
          ),
          JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys).result.head.map(
            r => assert("a" === r)
          ),
          // @>
          JsonTests.filter(_.json @> """ {"b":"aaa"} """.parseOption.getOrElse(jNull)).map(_.id).result.head.map(
            r => assert(33L === r)
          ),
          // <@
          JsonTests.filter(""" {"b":"aaa"} """.parseOption.getOrElse(jNull) <@: _.json).map(_.id).result.head.map(
            r => assert(33L === r)
          ),
          // {}_typeof
          JsonTests.filter(_.id === testRec1.id).map(_.json.+>("a").jsonType).result.head.map(
            r => assert("number" === r.toLowerCase)
          ),
          // ?
          JsonTests.filter(_.json ?? "b".bind).to[List].result.map(
            r => assert(List(testRec1, testRec3) === r)
          ),
          // ?|
          JsonTests.filter(_.json ?| List("a", "c").bind).to[List].result.map(
            r => assert(List(testRec1, testRec3) === r)
          ),
          // ?&
          JsonTests.filter(_.json ?& List("a", "c").bind).to[List].result.map(
            r => assert(List(testRec1) === r)
          ),
          // ||
          JsonTests.filter(_.id === 33L).map(_.json || """ {"d":"test"} """.parseOption.getOrElse(jNull)).result.head.map(
            r => assert(""" {"a": 101, "b": "aaa", "c": [3, 4, 5, 9], "d": "test"} """.replace(" ", "") === r.toString().replace(" ", ""))
          ),
          // -
          JsonTests.filter(_.id === 33L).map(_.json - "c".bind).result.head.map(
            r => assert(""" {"a": 101, "b": "aaa"} """.replace(" ", "") === r.toString().replace(" ", ""))
          ),
          // #-
          JsonTests.filter(_.id === 33L).map(_.json #- List("c")).result.head.map(
            r => assert(""" {"a": 101, "b": "aaa"} """.replace(" ", "") === r.toString().replace(" ", ""))
          ),
          // #-
          JsonTests.filter(_.id === 33L).map(_.json.set(List("c"), """ [1] """.parseOption.getOrElse(jNull).bind)).result.head.map(
            r => assert(""" {"a": 101, "b": "aaa", "c": [1]} """.replace(" ", "") === r.toString().replace(" ", ""))
          )
        )
      ).andFinally(
        JsonTests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  //------------------------------------------------------------------------------

  test("Argonaut json Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getJsonBeanResult = GetResult(r => JsonBean(r.nextLong(), r.nextJson()))

    val b = JsonBean(34L, """ { "a":101, "b":"aaa", "c":[3,4,5,9] } """.parseOption.getOrElse(jNull))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table JsonTest4(
              id int8 not null primary key,
              json #${MyPostgresProfile.pgjson} not null)
          """,
        ///
        sqlu""" insert into JsonTest4 values(${b.id}, ${b.json}) """,
        sql""" select * from JsonTest4 where id = ${b.id} """.as[JsonBean].head.map(
          r => assert(b === r)
        ),
        ///
        sqlu"drop table if exists JsonTest4 cascade"
      ).transactionally
    ),Duration.Inf)
  }
}
