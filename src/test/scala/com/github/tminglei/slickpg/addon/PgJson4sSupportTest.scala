package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import org.json4s._
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global

class PgJson4sSupportTest {
  import slick.driver.PostgresDriver

  object MyPostgresDriver extends PostgresDriver
                            with PgJson4sSupport
                            with array.PgArrayJdbcTypes {
    /// for json support
    override val pgjson = "jsonb"
    type DOCType = text.Document
    override val jsonMethods = org.json4s.native.JsonMethods

    override val api = new API with JsonImplicits {
      implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    }

    val plainAPI = new API with Json4sJsonPlainImplicits
  }

  ///
  import MyPostgresDriver.api._
  import MyPostgresDriver.jsonMethods._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: JValue)

  class JsonTestTable(tag: Tag) extends Table[JsonBean](tag, "JsonTest1") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[JValue]("json")

    def * = (id, json) <> (JsonBean.tupled, JsonBean.unapply)
  }
  val JsonTests = TableQuery[JsonTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = JsonBean(33L, parse(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))
  val testRec2 = JsonBean(35L, parse(""" [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """))
  val testRec3 = JsonBean(37L, parse(""" ["a", "b"] """))

  @Test
  def testJsonFunctions(): Unit = {
    val json1 = parse(""" {"a":"v1","b":2} """)
    val json2 = parse(""" {"a":"v5","b":3} """)

    db.run(DBIO.seq(
      JsonTests.schema create,
      ///
      JsonTests forceInsertAll List(testRec1, testRec2, testRec3),
      // 0. simple test
      JsonTests.filter(_.id === testRec2.id.bind).map(_.json).result.head.map(
        assertEquals(JArray(List(json1,json2)), _)
      ),
      // 1. '->>'/'->'/'#>'/'#>>' (note: json array's index starts with 0)
      JsonTests.filter(_.json.+>>("a") === "101").map(_.json.+>>("c")).result.head.map(
        r => assertEquals("[3,4,5,9]", r.replace(" ", ""))
      ),
      JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c")).result.head.map(
        assertEquals(JArray(List(JInt(3), JInt(4), JInt(5), JInt(9))), _)
      ),
      JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1)).result.head.map(
        assertEquals(json2, _)
      ),
      JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1)).result.head.map(
        r => assertEquals("""{"a":"v5","b":3}""", r.replace(" ", ""))
      ),
      JsonTests.filter(_.id === testRec1.id).map(_.json.#>(List("c"))).result.head.map(
        assertEquals(parse("[3,4,5,9]"), _)
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
      JsonTests.filter(_.json @> parse(""" {"b":"aaa"} """)).result.head.map(
        assertEquals(33L, _)
      ),
      JsonTests.filter(_.json @> parse(""" [{"a":"v5"}] """)).result.head.map(
        assertEquals(35L, _)
      ),
      JsonTests.filter(parse(""" {"b":"aaa"} """) <@: _.json).result.head.map(
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

    val b = JsonBean(37L, parse(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))

    db.run(DBIO.seq(
      sqlu"""create table JsonTest1(
            |  id int8 not null primary key,
            |  json json not null)
          """,
      ///
      sqlu"insert into JsonTest1 values(${b.id}, ${b.json})",
      sql"select * from JsonTest1 where id = ${b.id}".as[JsonBean].head.map(
        assertEquals(b, _)
      ),
      ///
      sqlu"drop table if exists JsonTest1 cascade"
    ).transactionally)
  }
}
