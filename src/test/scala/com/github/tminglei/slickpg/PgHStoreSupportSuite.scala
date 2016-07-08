package com.github.tminglei.slickpg

import org.scalatest.FunSuite
import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration._

class PgHStoreSupportSuite extends FunSuite {
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class MapBean(id: Long, hstore: Map[String, String])

  class HStoreTestTable(tag: Tag) extends Table[MapBean](tag, "HStoreTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def hstore = column[Map[String, String]]("hstoreMap", O.Default(Map.empty))

    def * = (id, hstore) <> (MapBean.tupled, MapBean.unapply)
  }
  val HStoreTests = TableQuery[HStoreTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = MapBean(33L, Map("a"->"val1", "b"->"val3", "c"->"321"))
  val testRec2 = MapBean(35L, Map("a"->"val7", "e"->"val33", "c"->"111"))
  val testRec3 = MapBean(37L, Map("a"->null, "c"->"105"))
  val testRec4 = MapBean(41L, Map.empty[String, String])

  test("Hstore Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        HStoreTests.schema create,
        ///
        HStoreTests forceInsertAll List(testRec1, testRec2, testRec3, testRec4)
      ).andThen(
        DBIO.seq(
          HStoreTests.to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3, testRec4) === r)
          ),
          // ->
          HStoreTests.filter(_.id === testRec1.id.bind).map(_.hstore.+>("a")).result.head.map(
            r => assert("val1" === r)
          ),
          HStoreTests.filter(_.hstore.+>("a") === "val7".bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2).map(_.hstore) === r.map(_.hstore))
          ),
          HStoreTests.filter(_.hstore.+>("c").asColumnOf[Long] === 111L.bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2).map(_.hstore) === r.map(_.hstore))
          ),
          // >>
          HStoreTests.filter(_.hstore.>>[Long]("c".bind) === 111L.bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2).map(_.hstore) === r.map(_.hstore))
          ),
          // ?
          HStoreTests.filter(_.hstore.??("a".bind)).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3).map(_.hstore) === r.map(_.hstore))
          ),
          // defined
          HStoreTests.filter(_.hstore.?*("a".bind)).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2).map(_.hstore) === r.map(_.hstore))
          ),
          // ?&
          HStoreTests.filter(_.hstore.?&(List("a").bind)).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3).map(_.hstore) === r.map(_.hstore))
          ),
          // ?|
          HStoreTests.filter(_.hstore.?|(List("a", "b", "c").bind)).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3).map(_.hstore) === r.map(_.hstore))
          ),
          // @>
          HStoreTests.filter(_.hstore @> Map("a"->"val7", "e"->"val33").bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2).map(_.hstore) === r.map(_.hstore))
          ),
          // <@
          HStoreTests.filter(Map("a"->"val7", "e"->"val33").bind <@: _.hstore).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2).map(_.hstore) === r.map(_.hstore))
          ),
          // +
          HStoreTests.filter(_.id === 37L).map(t => t.hstore @+ Map("a"->"test").bind).result.head.map(
            r => assert(Map("a"->"test", "c"->"105") === r)
          ),
          // -
          HStoreTests.filter(_.id === 37L).map(t => t.hstore @- Map("a"->"111", "c"->"105").bind).result.head.map(
            r => assert(Map("a"->null) === r)
          ),
          HStoreTests.filter(_.id === 37L).map(t => t.hstore -- List("a").bind).result.head.map(
            r => assert(Map("c"->"105") === r)
          ),
          HStoreTests.filter(_.id === 37L).map(t => t.hstore -/ "a".bind).result.head.map(
            r => assert(Map("c"->"105") === r)
          ),
          // slice
          HStoreTests.filter(_.id === 33L).map(t => t.hstore slice List("a", "b").bind).result.head.map(
            r => assert(Map("a"->"val1", "b"->"val3") === r)
          )
        )
      ).andFinally(
        HStoreTests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  //------------------------------------------------------------------------------

  test("Hstore Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getMapBeanResult = GetResult(r => MapBean(r.nextLong(), r.nextHStore()))

    val b = MapBean(34L, Map("a"->"val1", "b"->"val3", "c"->"321"))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table HStoreTest(
                id int8 not null primary key,
                hstoreMap hstore not null)
            """,
        ///
        sqlu""" insert into HStoreTest values(${b.id}, ${b.hstore}) """,
        sql""" select * from HStoreTest where id = ${b.id} """.as[MapBean].head.map(
          r => assert(b === r)
        ),
        ///
        sqlu"drop table if exists HStoreTest cascade"
      ).transactionally
    ), Duration.Inf)
  }
}
