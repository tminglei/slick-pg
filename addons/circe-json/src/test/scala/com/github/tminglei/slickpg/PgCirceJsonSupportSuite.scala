package com.github.tminglei.slickpg

import java.util.concurrent.Executors

import cats.syntax.either._
import io.circe._
import io.circe.parser._
import org.scalatest.FunSuite
import slick.jdbc.{GetResult, PostgresProfile}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class PgCirceJsonSupportSuite extends FunSuite {
  implicit val testExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  trait MyPostgresProfile extends PostgresProfile
                            with PgCirceJsonSupport
                            with array.PgArrayJdbcTypes {
    override val pgjson = "jsonb"

    override val api: API = new API {}

    val plainAPI = new API with CirceJsonPlainImplicits

    ///
    trait API extends super.API with JsonImplicits {
      implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    }
  }
  object MyPostgresProfile extends MyPostgresProfile

  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class JsonBean(id: Long, json: Json)

  class JsonTestTable(tag: Tag) extends Table[JsonBean](tag, "JsonTest5") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def json = column[Json]("json")

    def * = (id, json) <> (JsonBean.tupled, JsonBean.unapply)
  }
  val JsonTests = TableQuery[JsonTestTable]

  val testRec1 = JsonBean(33L, parse(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """).getOrElse(Json.Null))
  val testRec2 = JsonBean(35L, parse(""" [ {"a":"v1","b":2}, {"a":"v5","b":3} ] """).getOrElse(Json.Null))
  val testRec3 = JsonBean(37L, parse(""" ["a", "b"] """).getOrElse(Json.Null))

  test("Circe json Lifted support") {
    val json1 = parse(""" {"a":"v1","b":2} """).getOrElse(Json.Null)
    val json2 = parse(""" {"a":"v5","b":3} """).getOrElse(Json.Null)

    Await.result(db.run(
      DBIO.seq(
        JsonTests.schema create,
        JsonTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          JsonTests.filter(_.id === testRec2.id.bind).map(_.json).result.head.map(
            r => assert(Json.arr(json1, json2) === r)
          ),
          // ->>/->
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>>("c")).result.head.map(
            r => assert("[3,4,5,9]" === r.replace(" ", ""))
          ),
          JsonTests.filter(_.json.+>>("a") === "101".bind).map(_.json.+>("c")).result.head.map(
            r => assert(Json.arr(Json.fromLong(3), Json.fromLong(4), Json.fromLong(5), Json.fromLong(9)) === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>(1)).result.head.map(
            r => assert(json2 === r)
          ),
          JsonTests.filter(_.id === testRec2.id).map(_.json.~>>(1)).result.head.map(
            r => assert("""{"a":"v5","b":3}""" === r.replace(" ", ""))
          ),
          // #>>/#>
          JsonTests.filter(_.id === testRec1.id).map(_.json.#>(List("c"))).result.head.map(
            r => assert(Json.arr(Json.fromLong(3), Json.fromLong(4), Json.fromLong(5), Json.fromLong(9)) === r)
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
          JsonTests.filter(_.json @> parse(""" {"b":"aaa"} """).getOrElse(Json.Null)).map(_.id).result.head.map(
            r => assert(33L === r)
          ),
          // <@
          JsonTests.filter(parse(""" {"b":"aaa"} """).getOrElse(Json.Null) <@: _.json).map(_.id).result.head.map(
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

  test("Circe json Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getJsonBeanResult = GetResult(r => JsonBean(r.nextLong(), r.nextJson()))

    val b = JsonBean(34L, parse(""" { "a":101, "b":"aaa", "c":[3,4,5,9] } """).getOrElse(Json.Null))

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
