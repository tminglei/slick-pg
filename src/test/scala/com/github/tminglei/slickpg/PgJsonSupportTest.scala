package com.github.tminglei.slickpg

import org.junit.Assert._
import org.junit._

import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global

class PgJsonSupportTest {
  import MyPostgresDriver.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

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

  @Test
  def testJsonFunctions(): Unit = {
    val json1 = """{"a":"v1","b":2}"""
    val json2 = """{"a":"v5","b":3}"""

    db.run(DBIO.seq(
      JsonTests.schema create,
      ///
      JsonTests forceInsertAll List(testRec1, testRec2, testRec3),
      // 0. simple test
      JsonTests.filter(_.id === testRec2.id.bind).map(_.json).result.head.map(
        r => assertEquals("""[{"a":"v1","b":2},{"a":"v5","b":3}]""", r.value.replace(" ", ""))
      ),
      // 1. '->>'/'->'/'#>'/'#>>' (note: json array's index starts with 0)
      JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>>("c")).result.head.map(
        r => assertEquals("[3,4,5,9]", r.replace(" ", ""))
      ),
      JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c")).result.head.map(
        r => assertEquals("[3,4,5,9]", r.value.replace(" ", ""))
      ),
      JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1)).result.head.map(
        r => assertEquals("""{"a":"v5","b":3}""", r.replace(" ", ""))
      ),
      JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1)).result.head.map(
        r => assertEquals(json2, r.value.replace(" ", ""))
      ),
      JsonTests.filter(_.id === testRec1.id).map(_.json.#>(List("c"))).result.head.map(
        r => assertEquals("[3,4,5,9]", r.value.replace(" ", ""))
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
        r => assertEquals(List(json1, json2), r.map(_.value.replace(" ", "")))
      ),
      JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements).result.head.map(
        r => assertEquals(json1, r.value.replace(" ", ""))
      ),
      JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElementsText).result.head.map(
        r => assertEquals(json1, r.replace(" ", ""))
      ),
      // 4. '_object_keys'
      JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys).to[List].result.map(
        assertEquals(List("a","b","c"), _)
      ),
      JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys).result.head.map(
        assertEquals("a", _)
      ),
      // 5. '@>'/'<@'
      JsonTests.filter(_.json @> JsonString(""" {"b":"aaa"} """).bind).map(_.id).result.head.map(
        assertEquals(33L, _)
      ),
      JsonTests.filter(JsonString(""" {"b":"aaa"} """).bind <@: _.json).map(_.id).result.head.map(
        assertEquals(33L, _)
      ),
      // 6. '_typeof'
      JsonTests.filter(_.id === testRec1.id).map(_.json.+>("a").jsonType).result.head.map(
        r => assertEquals("number", r.toLowerCase)
      ),
      // 7. '?'/'?|'/'?&'
      JsonTests.filter(_.json ?? "b".bind).map(_.json).to[List].result.map(
        r => assertEquals(List(testRec1, testRec3).map(_.json.value.replace(" ", "")), r.map(_.value.replace(" ", "")))
      ),
      JsonTests.filter(_.json ?| List("a", "c").bind).map(_.json).to[List].result.map(
        r => assertEquals(List(testRec1, testRec3).map(_.json.value.replace(" ", "")), r.map(_.value.replace(" ", "")))
      ),
      JsonTests.filter(_.json ?& List("a", "c").bind).map(_.json).to[List].result.map(
        r => assertEquals(List(testRec1).map(_.json.value.replace(" ", "")), r.map(_.value.replace(" ", "")))
      ),
      ///
      JsonTests.schema drop
    ).transactionally)
  }

  //------------------------------------------------------------------------------

  @Test
  def testPlainJsonFunctions(): Unit = {
    import MyPlainPostgresDriver.plainAPI._

    implicit val getJsonBeanResult = GetResult(r => JsonBean(r.nextLong(), r.nextJson()))

    val b = JsonBean(37L, JsonString(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))

    db.run(DBIO.seq(
      sqlu"""create table JsonTest0(
            |  id int8 not null primary key,
            |  json json not null)
          """,
      ///
      sqlu"insert into JsonTest0 values(${b.id}, ${b.json})",
      sql"select * from JsonTest0 where id = ${b.id}".as[JsonBean].head.map(
        assertEquals(b, _)
      ),
      ///
      sqlu"drop table if exists JsonTest0 cascade"
    ).transactionally)
  }
}