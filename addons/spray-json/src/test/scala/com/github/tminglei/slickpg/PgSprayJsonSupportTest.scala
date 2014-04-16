package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import spray.json._
import DefaultJsonProtocol._

class PgSprayJsonSupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: JsValue)

  class JsonTestTable(tag: Tag) extends Table[JsonBean](tag, "JsonTest3") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[JsValue]("json")

    def * = (id, json) <> (JsonBean.tupled, JsonBean.unapply)
  }
  val JsonTests = TableQuery[JsonTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = JsonBean(33L, """ { "a":101, "b":"aaa", "c":[3,4,5,9] } """.asJson)
  val testRec2 = JsonBean(35L, """ [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """.asJson)

  @Test
  def testJsonFunctions(): Unit = {
    db withSession { implicit session: Session =>
      JsonTests forceInsertAll (testRec1, testRec2)

      val json1 = """ {"a":"v1","b":2} """.asJson
      val json2 = """ {"a":"v5","b":3} """.asJson

      val q0 = JsonTests.filter(_.id === testRec2.id.bind).map(_.json)
      println(s"[spray-json] sql0 = ${q0.selectStatement}")
      assertEquals(JsArray(List(json1,json2)), q0.first)

// pretty(render(JsNumber(101))) will get "101", but parse("101") will fail, since json string must start with '{' or '['
//      println(s"'+>' sql = ${q1.selectStatement}")
//      assertEquals(JsNumber(101), q1.first)

      val q11 = JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>>("c"))
      println(s"[spray-json] '+>>' sql = ${q11.selectStatement}")
      assertEquals("[3,4,5,9]", q11.first)

      val q12 = JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c"))
      println(s"[spray-json] '+>' sql = ${q12.selectStatement}")
      assertEquals(JsArray(List(JsNumber(3), JsNumber(4), JsNumber(5), JsNumber(9))), q12.first)

      // json array's index starts with 0
      val q2 = JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1))
      println(s"[spray-json] '~>' sql = ${q2.selectStatement}")
      assertEquals(json2, q2.first)

      val q21 = JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1))
      println(s"[spray-json] '~>>' sql = ${q21.selectStatement}")
      assertEquals("""{"a":"v5","b":3}""", q21.first)

      val q3 = JsonTests.filter(_.id === testRec2.id).map(_.json.arrayLength)
      println(s"[spray-json] 'arrayLength' sql = ${q3.selectStatement}")
      assertEquals(2, q3.first)

      val q4 = JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements)
      println(s"[spray-json] 'arrayElements' sql = ${q4.selectStatement}")
      assertEquals(List(json1, json2), q4.list)

      val q41 = JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements)
      println(s"[spray-json] 'arrayElements' sql = ${q41.selectStatement}")
      assertEquals(json1, q41.first)

      val q5 = JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys)
      println(s"[spray-json] 'objectKeys' sql = ${q5.selectStatement}")
      assertEquals(List("a","b","c"), q5.list)

      val q51 = JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys)
      println(s"[spray-json] 'objectKeys' sql = ${q51.selectStatement}")
      assertEquals("a", q51.first)
    }
  }

  //------------------------------------------------------------------------------

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      JsonTests.ddl create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      JsonTests.ddl drop
    }
  }
}
