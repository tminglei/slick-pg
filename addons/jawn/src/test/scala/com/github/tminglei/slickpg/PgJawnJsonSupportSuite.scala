package com.github.tminglei.slickpg

import java.util.concurrent.Executors

import org.typelevel.jawn._
import org.typelevel.jawn.ast._

import org.scalatest.funsuite.AnyFunSuite
import slick.jdbc.{GetResult, PostgresProfile}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class PgJawnJsonSupportSuite extends AnyFunSuite with PostgresContainer {
  implicit val testExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  trait MyPostgresProfile extends PostgresProfile
                            with PgJawnJsonSupport
                            with array.PgArrayJdbcTypes {
    override val pgjson = "jsonb"

    override val api: API = new API {}

    val plainAPI = new API with JawnJsonPlainImplicits

    ///
    trait API extends JdbcAPI with JsonImplicits {
      implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
      implicit val jsonArrayTypeMapper =
        new AdvancedArrayJdbcType[JValue](pgjson,
          (s) => utils.SimpleArrayUtils.fromString[JValue](JParser.parseUnsafe(_))(s).orNull,
          (v) => utils.SimpleArrayUtils.mkString[JValue](_.toString())(v)
        ).to(_.toList)
    }
  }
  object MyPostgresProfile extends MyPostgresProfile

  ///
  import MyPostgresProfile.api._

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: JValue, jsons: List[JValue])

  class JsonTestTable(tag: Tag) extends Table[JsonBean](tag, "JsonTest2") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[JValue]("json", O.Default(JParser.parseUnsafe(""" {"a":"v1","b":2} """)))
    def jsons = column[List[JValue]]("jsons")

    def * = (id, json, jsons) <> (JsonBean.tupled, JsonBean.unapply)
  }
  val JsonTests = TableQuery[JsonTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = JsonBean(33L, JParser.parseUnsafe(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """), List(JParser.parseUnsafe(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """)))
  val testRec2 = JsonBean(35L, JParser.parseUnsafe(""" [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """), List(JParser.parseUnsafe(""" [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """)))
  val testRec3 = JsonBean(37L, JParser.parseUnsafe(""" ["a", "b"] """), Nil)

  test("Jawn json Lifted support") {
    val json1 = JParser.parseUnsafe(""" {"a":"v1","b":2} """)
    val json2 = JParser.parseUnsafe(""" {"a":"v5","b":3} """)

    Await.result(db.run(
      DBIO.seq(
        JsonTests.schema create,
        ///
        JsonTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          JsonTests.filter(_.id === testRec2.id.bind).map(_.json).result.head.map(
            r => assert(JArray(Array(json1,json2)) === r)
          ),
          JsonTests.to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // ->>/->
          JsonTests.filter(_.json.+>>("a") === "101").map(_.json.+>>("c")).result.head.map(
            r => assert("[3,4,5,9]" === r.replace(" ", ""))
          ),
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c")).result.head.map(
            r => assert(JArray(Array(JNum(3), JNum(4), JNum(5), JNum(9))) === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1)).result.head.map(
            r => assert(json2 === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1)).result.head.map(
            r => assert("""{"a":"v5","b":3}""" === r.replace(" ", ""))
          ),
          // #>>/#>
          JsonTests.filter(_.id === testRec1.id).map(_.json.#>(List("c"))).result.head.map(
            r => assert(JParser.parseUnsafe("[3,4,5,9]") === r)
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
          JsonTests.filter(_.json @> JParser.parseUnsafe(""" {"b":"aaa"} """)).result.head.map(
            r => assert(33L === r.id)
          ),
          JsonTests.filter(_.json @> JParser.parseUnsafe(""" [{"a":"v5"}] """)).result.head.map(
            r => assert(35L === r.id)
          ),
          // <@
          JsonTests.filter(JParser.parseUnsafe(""" {"b":"aaa"} """) <@: _.json).result.head.map(
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
          ),
          // ||
          JsonTests.filter(_.id === 33L).map(_.json || JParser.parseUnsafe(""" {"d":"test"} """)).result.head.map(
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
          JsonTests.filter(_.id === 33L).map(_.json.set(List("c"), JParser.parseUnsafe(""" [1] """).bind)).result.head.map(
            r => assert(""" {"a": 101, "b": "aaa", "c": [1]} """.replace(" ", "") === r.toString().replace(" ", ""))
          )
        )
      ).andFinally(
        JsonTests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  //------------------------------------------------------------------------------

  case class JsonBean1(id: Long, json: JValue)

  test("Json Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getJsonBeanResult = GetResult(r => JsonBean1(r.nextLong(), r.nextJson()))

    val b = JsonBean1(34L, JParser.parseUnsafe(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table JsonTest2(
              id int8 not null primary key,
              json #${MyPostgresProfile.pgjson} not null)
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
