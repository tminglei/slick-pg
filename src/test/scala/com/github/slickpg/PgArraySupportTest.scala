package com.github.slickpg

import org.junit._
import org.junit.Assert._

class PgArraySupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class ArrayBean(
    id: Long,
    intArr: List[Int],
    longArr: List[Long],
    strArr: Option[List[String]]
    )

  object ArrayTestTable extends Table[ArrayBean](Some("test"), "ArrayTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def intArr = column[List[Int]]("intArray")
    def longArr = column[List[Long]]("longArray")
    def strArr = column[Option[List[String]]]("stringArray")

    def * = id ~ intArr ~ longArr ~ strArr <> (ArrayBean, ArrayBean unapply _)
  }

  //------------------------------------------------------------------------------

  val testRec1 = ArrayBean(33L, List(101, 102, 103), List(1L, 3L, 5L, 7L), Some(List("str1", "str3")))
  val testRec2 = ArrayBean(37L, List(101, 103), List(11L, 31L, 5L), Some(List("str11", "str3")))
  val testRec3 = ArrayBean(41L, List(103, 101), List(11L, 5L, 31L), Some(List("str11", "str5", "str3")))

  @Test
  def testSimpleInsertFetch(): Unit = {
    db withSession { implicit session: Session =>
      ArrayTestTable.insert(testRec1)

      val rec1 = ArrayTestTable.where(_.id === testRec1.id.bind).map(t => t).first()
      assertEquals(testRec1, rec1)
    }
  }

  @Test
  def testArrayFunctions(): Unit = {
    db withSession { implicit session: Session =>
      ArrayTestTable.insert(testRec1)
      ArrayTestTable.insert(testRec2)
      ArrayTestTable.insert(testRec3)

      val q1 = ArrayTestTable.where(101.bind === _.intArr.any).sortBy(_.id).map(t => t)
      println(s"'any' sql = ${q1.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q1.list())

      val q2 = ArrayTestTable.where(5L.bind <= _.longArr.all).sortBy(_.id).map(t => t)
      println(s"'all' sql = ${q2.selectStatement}")
      assertEquals(List(testRec2, testRec3), q2.list())

      /* notes: use 'Array("str3").bind' instead of 'Array("str3")' */
//      val q3 = ArrayTestTable.where(_.strArr @> Array("str3")).sortBy(_.id).map(t => t)
      val q3 = ArrayTestTable.where(_.strArr @> List("str3").bind).sortBy(_.id).map(t => t)
      println(s"'@>' sql = ${q3.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q3.list())

      val q31 = ArrayTestTable.where(List("str3").bind <@: _.strArr).sortBy(_.id).map(t => t)
      println(s"'<@' sql = ${q31.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q31.list())

      val q4 = ArrayTestTable.where(_.longArr @& List(5L, 17L).bind).sortBy(_.id).map(t => t)
      println(s"'&&' sql = ${q4.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q4.list())

      val q5 = ArrayTestTable.where(_.longArr.length() > 3.bind).sortBy(_.id).map(t => t)
      println(s"'length' sql = ${q5.selectStatement}")
      assertEquals(List(testRec1), q5.list())

      val q6 = ArrayTestTable.where(5L.bind <= _.longArr.all).map(_.strArr.unnest)
      println(s"'unnest' sql = ${q6.selectStatement}")
      assertEquals((testRec2.strArr.get ++ testRec3.strArr.get).toList, q6.list().map(_.orNull))
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      ArrayTestTable.ddl create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      ArrayTestTable.ddl drop
    }
  }
}
