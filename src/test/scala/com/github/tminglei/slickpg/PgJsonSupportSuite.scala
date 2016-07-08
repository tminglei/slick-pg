package com.github.tminglei.slickpg

import org.scalatest.FunSuite

import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration._

class PgJsonSupportSuite extends FunSuite {
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: JsonString)

  class JsonTestTable(tag: Tag) extends Table[JsonBean](tag, "JsonTest0") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[JsonString]("json", O.Default(JsonString(""" {"a":"v1","b":2} """)))

    def * = (id, json) <> (JsonBean.tupled, JsonBean.unapply)
  }
  val JsonTests = TableQuery[JsonTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = JsonBean(33L, JsonString(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))
  val testRec2 = JsonBean(35L, JsonString(""" [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """))
  val testRec3 = JsonBean(37L, JsonString(""" ["a", "b"] """))

  test("Json Lifted support") {
    val json1 = """{"a":"v1","b":2}"""
    val json2 = """{"a":"v5","b":3}"""

    Await.result(db.run(
      DBIO.seq(
        JsonTests.schema create,
        ///
        JsonTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          JsonTests.filter(_.id === testRec2.id.bind).map(_.json).result.head.map(
            r => assert("""[{"a":"v1","b":2},{"a":"v5","b":3}]""" === r.value.replace(" ", ""))
          ),
          // ->>/->
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>>("c")).result.head.map(
            r => assert("[3,4,5,9]" === r.replace(" ", ""))
          ),
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c")).result.head.map(
            r => assert("[3,4,5,9]" === r.value.replace(" ", ""))
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1)).result.head.map(
            r => assert("""{"a":"v5","b":3}""" === r.replace(" ", ""))
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1)).result.head.map(
            r => assert(json2 === r.value.replace(" ", ""))
          ),
          // #>>/#>
          JsonTests.filter(_.id === testRec1.id).map(_.json.#>(List("c"))).result.head.map(
            r => assert("[3,4,5,9]" === r.value.replace(" ", ""))
          ),
          JsonTests.filter(_.json.#>>(List("a")) === "101").result.head.map(
            r => assert(testRec1.json.value.replace(" ", "") === r.json.value.replace(" ", ""))
          ),
          // {}_array_length
          JsonTests.filter(_.id === testRec2.id).map(_.json.arrayLength).result.head.map(
            r => assert(2 === r)
          ),
          // {}_array_elements
          JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements).to[List].result.map(
            r => assert(List(json1, json2) === r.map(_.value.replace(" ", "")))
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements).result.head.map(
            r => assert(json1 === r.value.replace(" ", ""))
          ),
          // {}_array_elements_text
          JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElementsText).result.head.map(
            r => assert(json1 === r.replace(" ", ""))
          ),
          // {}_object_keys
          JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys).to[List].result.map(
            r => assert(List("a","b","c") === r)
          ),
          JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys).result.head.map(
            r => assert("a" === r)
          ),
          // @>
          JsonTests.filter(_.json @> JsonString(""" {"b":"aaa"} """).bind).map(_.id).result.head.map(
            r => assert(33L === r)
          ),
          // <@
          JsonTests.filter(JsonString(""" {"b":"aaa"} """).bind <@: _.json).map(_.id).result.head.map(
            r => assert(33L === r)
          ),
          // {}_typeof
          JsonTests.filter(_.id === testRec1.id).map(_.json.+>("a").jsonType).result.head.map(
            r => assert("number" === r.toLowerCase)
          ),
          // ?
          JsonTests.filter(_.json ?? "b".bind).map(_.json).to[List].result.map(
            r => assert(List(testRec1, testRec3).map(_.json.value.replace(" ", "")) === r.map(_.value.replace(" ", "")))
          ),
          // ?|
          JsonTests.filter(_.json ?| List("a", "c").bind).map(_.json).to[List].result.map(
            r => assert(List(testRec1, testRec3).map(_.json.value.replace(" ", "")) === r.map(_.value.replace(" ", "")))
          ),
          // ?&
          JsonTests.filter(_.json ?& List("a", "c").bind).map(_.json).to[List].result.map(
            r => assert(List(testRec1).map(_.json.value.replace(" ", "")) === r.map(_.value.replace(" ", "")))
          ),
          // ||
          JsonTests.filter(_.id === 33L).map(_.json || JsonString(""" {"d":"test"} """).bind).result.head.map(
            r => assert(""" {"a": 101, "b": "aaa", "c": [3, 4, 5, 9], "d": "test"} """.replace(" ", "") === r.value.replace(" ", ""))
          ),
          // -
          JsonTests.filter(_.id === 33L).map(_.json - "c".bind).result.head.map(
            r => assert(""" {"a": 101, "b": "aaa"} """.replace(" ", "") === r.value.replace(" ", ""))
          ),
          // #-
          JsonTests.filter(_.id === 33L).map(_.json #- List("c")).result.head.map(
            r => assert(""" {"a": 101, "b": "aaa"} """.replace(" ", "") === r.value.replace(" ", ""))
          ),
          // #-
          JsonTests.filter(_.id === 33L).map(_.json.set(List("c"), JsonString(""" [1] """))).result.head.map(
            r => assert(""" {"a": 101, "b": "aaa", "c": [1]} """.replace(" ", "") === r.value.replace(" ", ""))
          )
        )
      ).andFinally(
        JsonTests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  //------------------------------------------------------------------------------

  test("Json Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getJsonBeanResult = GetResult(r => JsonBean(r.nextLong(), r.nextJson()))

    val b = JsonBean(34L, JsonString(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table JsonTest0(
              id int8 not null primary key,
              json #${MyPostgresProfile.pgjson} not null)
          """,
        ///
        sqlu""" insert into JsonTest0 values(${b.id}, ${b.json}) """,
        sql""" select * from JsonTest0 where id = ${b.id} """.as[JsonBean].head.map(
          r => assert(b.json.value.replace(" ", "") === r.json.value.replace(" ", ""))
        ),
        ///
        sqlu"drop table if exists JsonTest0 cascade"
      ).transactionally
    ), Duration.Inf)
  }
}