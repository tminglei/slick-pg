package com.github.tminglei.slickpg

import org.junit.Assert._
import org.junit._

import scala.slick.jdbc.{StaticQuery => Q, GetResult}
import scala.util.Try

class PgJsonSupportTest {
  import com.github.tminglei.slickpg.MyPostgresDriver.simple._

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

  @Test
  def testJsonFunctions(): Unit = {
    db withSession { implicit session: Session =>
      Try { JsonTests.ddl drop }
      Try { JsonTests.ddl create }

      JsonTests forceInsertAll (testRec1, testRec2)

      val json1 = """{"a":"v1","b":2}"""
      val json2 = """{"a":"v5","b":3}"""

      val q0 = JsonTests.filter(_.id === testRec2.id.bind).map(_.json)
      println(s"[json] sql0 = ${q0.selectStatement}")
      assertEquals("""[{"a":"v1","b":2},{"a":"v5","b":3}]""", q0.first.value.replace(" ", ""))

// pretty(render(JsNumber(101))) will get "101", but parse("101") will fail, since json string must start with '{' or '['
//      println(s"'+>' sql = ${q1.selectStatement}")
//      assertEquals(JsNumber(101), q1.first)

      val q11 = JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>>("c"))
      println(s"[json] '+>>' sql = ${q11.selectStatement}")
      assertEquals("[3,4,5,9]", q11.first.value.replace(" ", ""))

      val q12 = JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c"))
      println(s"[json] '+>' sql = ${q12.selectStatement}")
      assertEquals("[3,4,5,9]", q12.first.value.replace(" ", ""))

      // json array's index starts with 0
      val q2 = JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1))
      println(s"[json] '~>' sql = ${q2.selectStatement}")
      assertEquals(json2, q2.first.value.replace(" ", ""))

      val q21 = JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1))
      println(s"[json] '~>>' sql = ${q21.selectStatement}")
      assertEquals("""{"a":"v5","b":3}""", q21.first)

      val q3 = JsonTests.filter(_.id === testRec2.id).map(_.json.arrayLength)
      println(s"[json] 'arrayLength' sql = ${q3.selectStatement}")
      assertEquals(2, q3.first)

      val q4 = JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements)
      println(s"[json] 'arrayElements' sql = ${q4.selectStatement}")
      assertEquals(List(json1, json2), q4.list.map(_.value.replace(" ", "")))

      val q41 = JsonTests.filter(_.id === testRec2.id).map(_.json.arrayElements)
      println(s"[json] 'arrayElements' sql = ${q41.selectStatement}")
      assertEquals(json1, q41.first.value.replace(" ", ""))

      val q5 = JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys)
      println(s"[json] 'objectKeys' sql = ${q5.selectStatement}")
      assertEquals(List("a","b","c"), q5.list)

      val q51 = JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys)
      println(s"[json] 'objectKeys' sql = ${q51.selectStatement}")
      assertEquals("a", q51.first)
    }
  }

  //------------------------------------------------------------------------------

  @Test
  def testPlainJsonFunctions(): Unit = {
    import MyPlainPostgresDriver.plainImplicits._

    implicit val getJsonBeanResult = GetResult(r => JsonBean(r.nextLong(), r.nextJson()))

    db withSession { implicit session: Session =>
      Try { Q.updateNA("drop table if exists JsonTest0 cascade").execute }
      Try {
        Q.updateNA("create table JsonTest0("+
          "id int8 not null primary key, "+
          "json json not null)"
        ).execute
      }

      val jsonBean = JsonBean(37L, JsonString(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))

      (Q.u + "insert into JsonTest0 values(" +? jsonBean.id + ", " +? jsonBean.json + ")").execute

      val found = (Q[JsonBean] + "select * from JsonTest0 where id = " +? jsonBean.id).first

      assertEquals(jsonBean, found)
    }
  }
}