package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import org.json4s._

class PgJsonSupportTest {
  import MyPostgresDriver.simple._
  import MyPostgresDriver.jsonMethods._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: JValue)

  object JsonTestTable extends Table[JsonBean]("JsonTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[JValue]("json")

    def * = id ~ json <> (JsonBean, JsonBean unapply _)
  }

  //------------------------------------------------------------------------------

  val testRec1 = JsonBean(33L, parse(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))
  val testRec2 = JsonBean(35L, parse(""" [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """))

  @Test
  def testJsonFunctions(): Unit = {
    db withSession { implicit session: Session =>
      JsonTestTable.insert(testRec1)
      JsonTestTable.insert(testRec2)

      val json1 = parse(""" {"a":"v1","b":2} """)
      val json2 = parse(""" {"a":"v5","b":3} """)

      val q0 = JsonTestTable.where(_.id === testRec2.id.bind).map(_.json)
      println(s"sql0 = ${q0.selectStatement}")
      assertEquals(JArray(List(json1,json2)), q0.first())

      // pretty(render(JInt(101))) will get "101", but parse("101") will fail, since json string must start with '{' or '['
//      val q1 = JsonTestTable.where(_.id === testRec1.id.bind).map(_.json.+>("a"))
//      println(s"'+>' sql = ${q1.selectStatement}")
//      assertEquals(JInt(101), q1.first())

      val q11 = JsonTestTable.where(_.json.+>>("a") === "101".bind).map(_.json.+>>("c"))
      println(s"'+>>' sql = ${q11.selectStatement}")
      assertEquals("[3,4,5,9]", q11.first())

      val q12 = JsonTestTable.where(_.json.+>>("a") === "101".bind).map(_.json.+>("c"))
      println(s"'+>' sql = ${q12.selectStatement}")
      assertEquals(JArray(List(JInt(3), JInt(4), JInt(5), JInt(9))), q12.first())

      // json array's index starts with 0
      val q2 = JsonTestTable.where(_.id === testRec2.id).map(_.json.~>(1))
      println(s"'~>' sql = ${q2.selectStatement}")
      assertEquals(json2, q2.first())

      val q21 = JsonTestTable.where(_.id === testRec2.id).map(_.json.~>>(1))
      println(s"'~>>' sql = ${q21.selectStatement}")
      assertEquals("""{"a":"v5","b":3}""", q21.first())

      val q3 = JsonTestTable.where(_.id === testRec2.id).map(_.json.arrayLength)
      println(s"'arrayLength' sql = ${q3.selectStatement}")
      assertEquals(2, q3.first())

      val q4 = JsonTestTable.where(_.id === testRec2.id).map(_.json.arrayElements)
      println(s"'arrayElements' sql = ${q4.selectStatement}")
      assertEquals(List(json1, json2), q4.list())

      val q41 = JsonTestTable.where(_.id === testRec2.id).map(_.json.arrayElements)
      println(s"'arrayElements' sql = ${q41.selectStatement}")
      assertEquals(json1, q41.first())

      val q5 = JsonTestTable.where(_.id === testRec1.id).map(_.json.objectKeys)
      println(s"'objectKeys' sql = ${q5.selectStatement}")
      assertEquals(List("a","b","c"), q5.list())

      val q51 = JsonTestTable.where(_.id === testRec1.id).map(_.json.objectKeys)
      println(s"'objectKeys' sql = ${q51.selectStatement}")
      assertEquals("a", q51.first())
    }
  }

  //------------------------------------------------------------------------------

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      JsonTestTable.ddl create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      JsonTestTable.ddl drop
    }
  }
}
