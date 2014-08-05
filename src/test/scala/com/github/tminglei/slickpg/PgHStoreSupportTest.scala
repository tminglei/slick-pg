package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import scala.util.Try

class PgHStoreSupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=postgres", driver = "org.postgresql.Driver")

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

  @Test
  def testHStoreFunctions(): Unit = {
    db withSession { implicit session: Session =>
      HStoreTests forceInsertAll (testRec1, testRec2, testRec3, testRec4)

      assertEquals(List(testRec1, testRec2, testRec3, testRec4), HStoreTests.list)

      val q1 = HStoreTests.filter(_.id === testRec1.id.bind).map(_.hstore.+>("a"))
      println(s"[hstore] '+>' sql = ${q1.selectStatement}")
      assertEquals("val1", q1.first)

      val q11 = HStoreTests.filter(_.hstore.+>("a") === "val7".bind).sortBy(_.id).map(t => t)
      println(s"[hstore] '+>' sql = ${q11.selectStatement}")
      assertEquals(List(testRec2).map(_.hstore), q11.list.map(_.hstore))

      val q12 = HStoreTests.filter(_.hstore.+>("c").asColumnOf[Long] === 111L.bind).sortBy(_.id).map(t => t)
      println(s"[hstore] '+>' sql = ${q12.selectStatement}")
      assertEquals(List(testRec2).map(_.hstore), q12.list.map(_.hstore))

      val q13 = HStoreTests.filter(_.hstore.>>[Long]("c".bind) === 111L.bind).sortBy(_.id).map(t => t)
      println(s"[hstore] '>>' sql = ${q13.selectStatement}")
      assertEquals(List(testRec2).map(_.hstore), q13.list.map(_.hstore))

      val q2 = HStoreTests.filter(_.hstore.??("c".bind)).sortBy(_.id).map(t => t)
      println(s"[hstore] '??' sql = ${q2.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3).map(_.hstore), q2.list.map(_.hstore))

      val q3 = HStoreTests.filter(_.hstore.?&("a".bind)).sortBy(_.id).map(t => t)
      println(s"[hstore] '?&' sql = ${q3.selectStatement}")
      assertEquals(List(testRec1, testRec2).map(_.hstore), q3.list.map(_.hstore))

      /* notes: use 'Map(..).bind' instead of 'Map(..)' */
//      val q4 = HStoreTests.filter(_.hstore @> Map("a"->"val7", "e"->"val33")).sortBy(_.id).map(t => t)
      val q4 = HStoreTests.filter(_.hstore @> Map("a"->"val7", "e"->"val33").bind).sortBy(_.id).map(t => t)
      println(s"[hstore] '@>' sql = ${q4.selectStatement}")
      assertEquals(List(testRec2).map(_.hstore), q4.list.map(_.hstore))

      val q41 = HStoreTests.filter(Map("a"->"val7", "e"->"val33").bind <@: _.hstore).sortBy(_.id).map(t => t)
      println(s"[hstore] '<@' sql = ${q41.selectStatement}")
      assertEquals(List(testRec2).map(_.hstore), q41.list.map(_.hstore))

      ///
      val q5 = HStoreTests.filter(_.id === 37L).map(t => t.hstore @+ Map("a"->"test").bind)
      println(s"[hstore] '@+' sql = ${q5.selectStatement}")
      assertEquals(Map("a"->"test", "c"->"105"), q5.first)

      val q6 = HStoreTests.filter(_.id === 37L).map(t => t.hstore @- Map("a"->"111", "c"->"105").bind)
      println(s"[hstore] '@-' sql = ${q6.selectStatement}")
      assertEquals(Map("a"->null), q6.first)

      val q7 = HStoreTests.filter(_.id === 37L).map(t => t.hstore -- List("a").bind)
      println(s"[hstore] '--' sql = ${q7.selectStatement}")
      assertEquals(Map("c"->"105"), q7.first)

      val q8 = HStoreTests.filter(_.id === 37L).map(t => t.hstore -/ "a".bind)
      println(s"[hstore] '-/' sql = ${q8.selectStatement}")
      assertEquals(Map("c"->"105"), q8.first)
    }
  }

  //------------------------------------------------------------------------------

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      Try { HStoreTests.ddl drop }
      Try { HStoreTests.ddl.createStatements.foreach(s => println(s"[hstore] $s")) }
      Try { HStoreTests.ddl create }
    }
  }
}
