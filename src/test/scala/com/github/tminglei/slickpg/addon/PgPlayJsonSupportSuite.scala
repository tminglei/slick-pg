package com.github.tminglei.slickpg

import org.scalatest.FunSuite
import play.api.libs.json._
import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration._

class PgPlayJsonSupportSuite extends FunSuite {
  import slick.driver.PostgresDriver

  object MyPostgresDriver extends PostgresDriver
                            with PgPlayJsonSupport
                            with array.PgArrayJdbcTypes {
    override val pgjson = "jsonb"

    override val api = new API with JsonImplicits {
      implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
      implicit val json4sJsonArrayTypeMapper =
        new AdvancedArrayJdbcType[JsValue](pgjson,
          (s) => utils.SimpleArrayUtils.fromString[JsValue](Json.parse(_))(s).orNull,
          (v) => utils.SimpleArrayUtils.mkString[JsValue](_.toString())(v)
        ).to(_.toList)
    }

    val plainAPI = new API with PlayJsonPlainImplicits
  }

  ///
  import MyPostgresDriver.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: JsValue, jsons: List[JsValue])

  class JsonTestTable(tag: Tag) extends Table[JsonBean](tag, "JsonTest2") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[JsValue]("json", O.Default(Json.parse(""" {"a":"v1","b":2} """)))
    def jsons = column[List[JsValue]]("jsons")

    def * = (id, json, jsons) <> (JsonBean.tupled, JsonBean.unapply)
  }
  val JsonTests = TableQuery[JsonTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = JsonBean(33L, Json.parse(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """), List(Json.parse(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """)))
  val testRec2 = JsonBean(35L, Json.parse(""" [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """), List(Json.parse(""" [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """)))
  val testRec3 = JsonBean(37L, Json.parse(""" ["a", "b"] """), Nil)

  test("Play json Lifted support") {
    val json1 = Json.parse(""" {"a":"v1","b":2} """)
    val json2 = Json.parse(""" {"a":"v5","b":3} """)

    Await.result(db.run(
      DBIO.seq(
        JsonTests.schema create,
        ///
        JsonTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          JsonTests.filter(_.id === testRec2.id.bind).map(_.json).result.head.map(
            r => assert(JsArray(List(json1,json2)) === r)
          ),
          JsonTests.to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // ->>/->
          JsonTests.filter(_.json.+>>("a") === "101").map(_.json.+>>("c")).result.head.map(
            r => assert("[3,4,5,9]" === r.replace(" ", ""))
          ),
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c")).result.head.map(
            r => assert(JsArray(List(JsNumber(3), JsNumber(4), JsNumber(5), JsNumber(9))) === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1)).result.head.map(
            r => assert(json2 === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1)).result.head.map(
            r => assert("""{"a":"v5","b":3}""" === r.replace(" ", ""))
          ),
          // #>>/#>
          JsonTests.filter(_.id === testRec1.id).map(_.json.#>(List("c"))).result.head.map(
            r => assert(Json.parse("[3,4,5,9]") === r)
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
          JsonTests.filter(_.json @> Json.parse(""" {"b":"aaa"} """)).result.head.map(
            r => assert(33L === r.id)
          ),
          JsonTests.filter(_.json @> Json.parse(""" [{"a":"v5"}] """)).result.head.map(
            r => assert(35L === r.id)
          ),
          // <@
          JsonTests.filter(Json.parse(""" {"b":"aaa"} """) <@: _.json).result.head.map(
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

  case class JsonBean1(id: Long, json: JsValue)

  test("Json Plain SQL support") {
    import MyPostgresDriver.plainAPI._

    implicit val getJsonBeanResult = GetResult(r => JsonBean1(r.nextLong(), r.nextJson()))

    val b = JsonBean1(34L, Json.parse(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table JsonTest2(
              id int8 not null primary key,
              json #${MyPostgresDriver.pgjson} not null)
          """,
        ///
        sqlu""" insert into JsonTest2 values(${b.id}, ${b.json}) """,
        sql""" select * from JsonTest2 where id = ${b.id} """.as[JsonBean1].head.map(
          r => assert(b === r)
        ),
        ///
        sqlu"drop table if exists JsonTest2 cascade"
      ).transactionally
    ), Duration.Inf)
  }
}