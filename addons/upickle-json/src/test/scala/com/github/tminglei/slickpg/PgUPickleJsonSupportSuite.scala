package com.github.tminglei.slickpg

import java.util.concurrent.Executors
import org.scalatest.funsuite.AnyFunSuite
import slick.jdbc.{GetResult, PostgresProfile}

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService}
import scala.concurrent.duration._

class PgUPickleJsonSupportSuite extends AnyFunSuite with PostgresContainer {
  implicit val testExecContext: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  trait MyPostgresProfile extends PostgresProfile
                            with PgUPickleJsonSupport
                            with array.PgArrayJdbcTypes {
    override val pgjson = "jsonb"

    override val api: API = new API {}

    val plainAPI = new API with UPickleJsonPlainImplicits

    ///
    trait API extends JdbcAPI with JsonImplicits {
      implicit val strListTypeMapper: DriverJdbcType[List[String]] = new SimpleArrayJdbcType[String]("text").to(_.toList)
    }
  }
  object MyPostgresProfile extends MyPostgresProfile

  import MyPostgresProfile.api._

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: ujson.Value)

  class JsonTestTable(tag: Tag) extends Table[JsonBean](tag, "JsonTest5") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[ujson.Value]("json")

    def * = (id, json) <> ((JsonBean.apply _).tupled, JsonBean.unapply)
  }
  val JsonTests = TableQuery[JsonTestTable]

  val testRec1 = JsonBean(33L, ujson.read(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))
  val testRec2 = JsonBean(35L, ujson.read(""" [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """))
  val testRec3 = JsonBean(37L, ujson.read(""" ["a", "b"] """))

  test("uPickle json Lifted support") {
    val json1 = ujson.read(""" {"a":"v1","b":2} """)
    val json2 = ujson.read(""" {"a":"v5","b":3} """)

    Await.result(db.run(
      DBIO.seq(
        JsonTests.schema create,
        JsonTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          JsonTests.filter(_.id === testRec2.id.bind).map(_.json).result.head.map(
            r => assert(ujson.Arr(json1, json2) === r)
          ),
          // ->>/->
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>>("c")).result.head.map(
            r => assert("[3,4,5,9]" === r.replace(" ", ""))
          ),
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c")).result.head.map(
            r => assert(ujson.Arr(ujson.Num(3), ujson.Num(4), ujson.Num(5), ujson.Num(9)) === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1)).result.head.map(
            r => assert(json2 === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1)).result.head.map(
            r => assert("""{"a":"v5","b":3}""" === r.replace(" ", ""))
          ),
          // #>>/#>
          JsonTests.filter(_.id === testRec1.id).map(_.json.#>(List("c"))).result.head.map(
            r => assert(ujson.Arr(ujson.Num(3), ujson.Num(4), ujson.Num(5), ujson.Num(9)) === r)
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
            r => assert(json1.toString.replace(" ", "").replace("\n", "") === r.replace(" ", ""))
          ),
          // {}_object_keys
          JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys).to[List].result.map(
            r => assert(List("a","b","c") === r)
          ),
          JsonTests.filter(_.id === testRec1.id).map(_.json.objectKeys).result.head.map(
            r => assert("a" === r)
          ),
          // @>
          JsonTests.filter(_.json @> ujson.read(""" {"b":"aaa"} """)).map(_.id).result.head.map(
            r => assert(33L === r)
          ),
          // <@
          JsonTests.filter(ujson.read(""" {"b":"aaa"} """) <@: _.json).map(_.id).result.head.map(
            r => assert(33L === r)
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

  test("UPickle json Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getJsonBeanResult: GetResult[JsonBean] = GetResult(r => JsonBean(r.nextLong(), r.nextJson()))

    val b = JsonBean(34L, ujson.read(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table JsonTest5(
              id int8 not null primary key,
              json #${MyPostgresProfile.pgjson} not null)
          """,
        sqlu""" insert into JsonTest5 values(${b.id}, ${b.json}) """,
        sql""" select * from JsonTest5 where id = ${b.id} """.as[JsonBean].head.map(
          r => assert(b === r)
        ),
        sqlu"drop table if exists JsonTest5 cascade"
      ).transactionally
    ), Duration.Inf)
  }
}
