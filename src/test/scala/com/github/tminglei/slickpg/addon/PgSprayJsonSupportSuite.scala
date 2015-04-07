package com.github.tminglei.slickpg

import org.scalatest.FunSuite
import spray.json._
import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration._

class PgSprayJsonSupportSuite extends FunSuite {
  import slick.driver.PostgresDriver

  object MyPostgresDriver extends PostgresDriver
                            with PgSprayJsonSupport
                            with array.PgArrayJdbcTypes {
    override val pgjson = "jsonb"

    override val api = new API with JsonImplicits {
      implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    }

    val plainAPI = new API with SprayJsonPlainImplicits
  }

  ///
  import MyPostgresDriver.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: JsValue)

  class JsonTestTable(tag: Tag) extends Table[JsonBean](tag, "JsonTest3") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[JsValue]("json")

    def * = (id, json) <> (JsonBean.tupled, JsonBean.unapply)
  }
  val JsonTests = TableQuery[JsonTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = JsonBean(33L, """ { "a":101, "b":"aaa", "c":[3,4,5,9] } """.parseJson)
  val testRec2 = JsonBean(35L, """ [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """.parseJson)
  val testRec3 = JsonBean(37L, """ ["a", "b"] """.parseJson)

  test("Spray json Lifted support") {
    val json1 = """ {"a":"v1","b":2} """.parseJson
    val json2 = """ {"a":"v5","b":3} """.parseJson

    Await.result(db.run(
      DBIO.seq(
        JsonTests.schema create,
        ///
        JsonTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          JsonTests.filter(_.id === testRec2.id.bind).map(_.json).result.head.map(
            r => assert(JsArray(json1,json2) === r)
          ),
          // ->>/->
          JsonTests.filter(_.json.+>>("a") === "101").map(_.json.+>>("c")).result.head.map(
            r => assert("[3,4,5,9]" === r.replace(" ", ""))
          ),
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c")).result.head.map(
            r => assert(JsArray(JsNumber(3), JsNumber(4), JsNumber(5), JsNumber(9)) === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1)).result.head.map(
            r => assert(json2 === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1)).result.head.map(
            r => assert("""{"a":"v5","b":3}""" === r.replace(" ", ""))
          ),
          // #>>/#>
          JsonTests.filter(_.id === testRec1.id).map(_.json.#>(List("c"))).result.head.map(
            r => assert("[3,4,5,9]".parseJson === r)
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
          JsonTests.filter(_.json @> """ {"b":"aaa"} """.parseJson).result.head.map(
            r => assert(33L === r.id)
          ),
          JsonTests.filter(_.json @> """ [{"a":"v5"}] """.parseJson).result.head.map(
            r => assert(35L === r.id)
          ),
          // <@
          JsonTests.filter(""" {"b":"aaa"} """.parseJson <@: _.json).result.head.map(
            r => assert(33L === r.id)
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
          )
        )
      ).andFinally(
        JsonTests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  //------------------------------------------------------------------------------

  test("Spary json Plain SQL support") {
    import MyPostgresDriver.plainAPI._

    implicit val getJsonBeanResult = GetResult(r => JsonBean(r.nextLong(), r.nextJson()))

    val b = JsonBean(34L, """ { "a":101, "b":"aaa", "c":[3,4,5,9] } """.parseJson)

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table JsonTest3(
              id int8 not null primary key,
              json #${MyPostgresDriver.pgjson} not null)
          """,
        ///
        sqlu""" insert into JsonTest3 values(${b.id}, ${b.json}) """,
        sql""" select * from JsonTest3 where id = ${b.id} """.as[JsonBean].head.map(
          r => assert(b === r)
        ),
        ///
        sqlu"drop table if exists JsonTest3 cascade"
      ).transactionally
    ), Duration.Inf)
  }
}
