package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import play.api.libs.json._
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global

class PgPlayJsonSupportTest {
  import slick.driver.PostgresDriver

  object MyPostgresDriver extends PostgresDriver
                            with PgPlayJsonSupport
                            with array.PgArrayJdbcTypes {
    override val pgjson = "jsonb"

    override val api = new API with JsonImplicits {
      implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    }

    val plainAPI = new API with PlayJsonPlainImplicits
  }

  ///
  import MyPostgresDriver.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: JsValue)

  class JsonTestTable(tag: Tag) extends Table[JsonBean](tag, "JsonTest2") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[JsValue]("json", O.Default(Json.parse(""" {"a":"v1","b":2} """)))

    def * = (id, json) <> (JsonBean.tupled, JsonBean.unapply)
  }
  val JsonTests = TableQuery[JsonTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = JsonBean(33L, Json.parse(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))
  val testRec2 = JsonBean(35L, Json.parse(""" [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """))
  val testRec3 = JsonBean(37L, Json.parse(""" ["a", "b"] """))

  @Test
  def testJsonFunctions(): Unit = {
    val json1 = Json.parse(""" {"a":"v1","b":2} """)
    val json2 = Json.parse(""" {"a":"v5","b":3} """)

    db.run(DBIO.seq(
      JsonTests.schema create,
      ///
      JsonTests forceInsertAll List(testRec1, testRec2, testRec3),
      // 0. simple test
      JsonTests.filter(_.id === testRec2.id.bind).map(_.json).result.head.map(
        assertEquals(JsArray(List(json1,json2)), _)
      ),
      // 1. '->>'/'->'/'#>'/'#>>' (note: json array's index starts with 0)
      JsonTests.filter(_.json.+>>("a") === "101").map(_.json.+>>("c")).result.head.map(
        r => assertEquals("[3,4,5,9]", r.replace(" ", ""))
      ),
      JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c")).result.head.map(
        assertEquals(JsArray(List(JsNumber(3), JsNumber(4), JsNumber(5), JsNumber(9))), _)
      ),
      JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1)).result.head.map(
        assertEquals(json2, _)
      ),
      JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1)).result.head.map(
        r => assertEquals("""{"a":"v5","b":3}""", r.replace(" ", ""))
      ),
      JsonTests.filter(_.id === testRec1.id).map(_.json.#>(List("c"))).result.head.map(
        assertEquals(Json.parse("[3,4,5,9]"), _)
      ),
      JsonTests.filter(_.json.#>>(List("a")) === "101").result.head.map(
        assertEquals(testRec1, _)
      ),
      // 2. '_array_length'
      JsonTests.filter(_.id === testRec2.id).map(_.json.arrayLength).result.head.map(
        assertEquals(2, _)
      ),
      // 3. '_array_elements'/'_array_elements_text'
      JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements).to[List].result.map(
        assertEquals(List(json1, json2), _)
      ),
      JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements).result.head.map(
        assertEquals(json1, _)
      ),
      JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElementsText).result.head.map(
        r => assertEquals(json1.toString.replace(" ", ""), r.replace(" ", ""))
      ),
      // 4. '_object_keys'
      JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys).to[List].result.map(
        assertEquals(List("a","b","c"), _)
      ),
      JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys).result.head.map(
        assertEquals("a", _)
      ),
      // 5. '@>'/'<@'
      JsonTests.filter(_.json @> Json.parse(""" {"b":"aaa"} """)).result.head.map(
        assertEquals(33L, _)
      ),
      JsonTests.filter(_.json @> Json.parse(""" [{"a":"v5"}] """)).result.head.map(
        assertEquals(35L, _)
      ),
      JsonTests.filter(Json.parse(""" {"b":"aaa"} """) <@: _.json).result.head.map(
        assertEquals(33L, _)
      ),
      // 6. '_typeof'
      JsonTests.filter(_.id === testRec1.id).map(_.json.+>("a").jsonType).result.head.map(
        r => assertEquals("number", r.toLowerCase)
      ),
      // 7. '?'/'?|'/'?&'
      JsonTests.filter(_.json ?? "b".bind).to[List].result.map(
        assertEquals(List(testRec1, testRec3), _)
      ),
      JsonTests.filter(_.json ?| List("a", "c").bind).to[List].result.map(
        assertEquals(List(testRec1, testRec3), _)
      ),
      JsonTests.filter(_.json ?& List("a", "c").bind).to[List].result.map(
        assertEquals(List(testRec1), _)
      ),
      ///
      JsonTests.schema drop
    ).transactionally)
  }

  //------------------------------------------------------------------------------

  @Test
  def testPlainJsonFunctions(): Unit = {
    import MyPostgresDriver.plainAPI._

    implicit val getJsonBeanResult = GetResult(r => JsonBean(r.nextLong(), r.nextJson()))

    val b = JsonBean(37L, Json.parse(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))

    db.run(DBIO.seq(
      sqlu"""create table JsonTest2(
            |  id int8 not null primary key,
            |  json json not null)
          """,
      ///
      sqlu"insert into JsonTest2 values(${b.id}, ${b.json})",
      sql"select * from JsonTest2 where id = ${b.id}".as[JsonBean].head.map(
        assertEquals(b, _)
      ),
      ///
      sqlu"drop table if exists JsonTest2 cascade"
    ).transactionally)
  }
}