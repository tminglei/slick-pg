package com.github.slickpg

import org.junit._
import org.junit.Assert._

class PgHStoreSupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class MapBean(id: Long, hstore: Map[String, String])

  object HStoreTestTable extends Table[MapBean](Some("test"), "HStoreTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def hstore = column[Map[String, String]]("hstoreMap")

    def * = id ~ hstore <> (MapBean, MapBean unapply _)
  }

  //------------------------------------------------------------------------------

  val testRec1 = MapBean(33L, Map("a"->"val1", "b"->"val3", "c"->"321"))
  val testRec2 = MapBean(35L, Map("a"->"val7", "e"->"val33", "c"->"111"))
  val testRec3 = MapBean(37L, Map("a"->null, "c"->"105"))
  val testRec4 = MapBean(41L, Map.empty[String, String])

  @Test
  def testSimpleInsertFetch(): Unit = {
    db withSession { implicit session: Session =>
      HStoreTestTable.insert(testRec1)
      HStoreTestTable.insert(testRec4)

      val rec1 = HStoreTestTable.where(_.id === testRec1.id.bind).map(t => t).first()
      assertEquals(testRec1.hstore, rec1.hstore)

      val rec4 = HStoreTestTable.where(_.id === testRec4.id.bind).map(t => t).first()
      assertEquals(testRec4.hstore, rec4.hstore)
    }
  }

  @Test
  def testHStoreFunctions(): Unit = {
    db withSession { implicit session: Session =>
      HStoreTestTable.insert(testRec1)
      HStoreTestTable.insert(testRec2)
      HStoreTestTable.insert(testRec3)

      val q1 = HStoreTestTable.where(_.id === testRec1.id.bind).map(_.hstore.+>("a"))
      println(s"'+>' sql = ${q1.selectStatement}")
      assertEquals("val1", q1.first())

      val q11 = HStoreTestTable.where(_.hstore.+>("a") === "val7".bind).sortBy(_.id).map(t => t)
      println(s"'+>' sql = ${q11.selectStatement}")
      assertEquals(List(testRec2).map(_.hstore), q11.list().map(_.hstore))

      val q12 = HStoreTestTable.where(_.hstore.+>("c").asColumnOf[Long] === 111L.bind).sortBy(_.id).map(t => t)
      println(s"'+>' sql = ${q12.selectStatement}")
      assertEquals(List(testRec2).map(_.hstore), q12.list().map(_.hstore))

      val q13 = HStoreTestTable.where(_.hstore.>>[Long]("c".bind) === 111L.bind).sortBy(_.id).map(t => t)
      println(s"'>>' sql = ${q13.selectStatement}")
      assertEquals(List(testRec2).map(_.hstore), q13.list().map(_.hstore))

      val q2 = HStoreTestTable.where(_.hstore.??("c".bind)).sortBy(_.id).map(t => t)
      println(s"'??' sql = ${q2.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3).map(_.hstore), q2.list().map(_.hstore))

      val q3 = HStoreTestTable.where(_.hstore.?&("a".bind)).sortBy(_.id).map(t => t)
      println(s"'?&' sql = ${q3.selectStatement}")
      assertEquals(List(testRec1, testRec2).map(_.hstore), q3.list().map(_.hstore))

      /* notes: use 'Map(..).bind' instead of 'Map(..)' */
//      val q4 = HStoreTestTable.where(_.hstore @> Map("a"->"val7", "e"->"val33")).sortBy(_.id).map(t => t)
      val q4 = HStoreTestTable.where(_.hstore @> Map("a"->"val7", "e"->"val33").bind).sortBy(_.id).map(t => t)
      println(s"'@>' sql = ${q4.selectStatement}")
      assertEquals(List(testRec2).map(_.hstore), q4.list().map(_.hstore))

      val q41 = HStoreTestTable.where(Map("a"->"val7", "e"->"val33").bind <@: _.hstore).sortBy(_.id).map(t => t)
      println(s"'<@' sql = ${q41.selectStatement}")
      assertEquals(List(testRec2).map(_.hstore), q41.list().map(_.hstore))

      ///
      val q5 = HStoreTestTable.where(_.id === 37L).map(t => t.hstore @+ Map("a"->"test").bind)
      println(s"'@+' sql = ${q5.selectStatement}")
      assertEquals(Map("a"->"test", "c"->"105"), q5.first())

      val q6 = HStoreTestTable.where(_.id === 37L).map(t => t.hstore @- "a".bind)
      println(s"'@-' sql = ${q6.selectStatement}")
      assertEquals(Map("c"->"105"), q6.first())

      val q7 = HStoreTestTable.where(_.id === 37L).map(t => t.hstore -- List("a").bind)
      println(s"'--' sql = ${q7.selectStatement}")
      assertEquals(Map("c"->"105"), q7.first())

      val q8 = HStoreTestTable.where(_.id === 37L).map(t => t.hstore -/ Map("a"->"111", "c"->"105").bind)
      println(s"'-/' sql = ${q8.selectStatement}")
      assertEquals(Map("a"->null), q8.first())
    }
  }

  //------------------------------------------------------------------------------

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      HStoreTestTable.ddl create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      HStoreTestTable.ddl drop
    }
  }
}
