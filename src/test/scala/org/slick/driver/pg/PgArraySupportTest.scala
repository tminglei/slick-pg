package org.slick.driver
package pg

import org.junit._
import org.junit.Assert._
import java.util.UUID

class PgArraySupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class ArrayBean(
    id: Long,
    uuidArr: List[UUID],
    longArr: List[Long],
    strArr: Option[List[String]]
    )

  object ArrayTestTable extends Table[ArrayBean](Some("test"), "ArrayTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def uuidArr = column[List[UUID]]("uuidArray")
    def longArr = column[List[Long]]("longArray")
    def strArr = column[Option[List[String]]]("stringArray")

    def * = id ~ uuidArr ~ longArr ~ strArr <> (ArrayBean, ArrayBean unapply _)
  }

  //------------------------------------------------------------------------------

  val uuid1 = UUID.randomUUID()
  val uuid2 = UUID.randomUUID()
  val uuid3 = UUID.randomUUID()

  val testRec1 = ArrayBean(33L, List(uuid1, uuid2, uuid3), List(1L, 3L, 5L, 7L), Some(List("str1", "str3")))
  val testRec2 = ArrayBean(37L, List(uuid1, uuid3), List(11L, 31L, 5L), Some(List("str11", "str3")))
  val testRec3 = ArrayBean(41L, List(uuid3, uuid1), List(11L, 5L, 31L), Some(List("str11", "str5", "str3")))

  @Test
  def testSimpleInsertFetch(): Unit = {
    db withSession { implicit session: Session =>
      ArrayTestTable.insert(testRec1)

      val rec1 = ArrayTestTable.where(_.id === testRec1.id.bind).map(t => t).first()
      assertEquals(testRec1.uuidArr, rec1.uuidArr)
      assertEquals(testRec1.longArr, rec1.longArr)
      assertEquals(testRec1.strArr.orNull, rec1.strArr.orNull)
    }
  }

  @Test
  def testArrayFunctions(): Unit = {
    db withSession { implicit session: Session =>
      ArrayTestTable.insert(testRec1)
      ArrayTestTable.insert(testRec2)
      ArrayTestTable.insert(testRec3)

      val q1 = ArrayTestTable.where(uuid1.bind === _.uuidArr.any).sortBy(_.id).map(t => t)
      println(s"'any' sql = ${q1.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3).map(_.uuidArr), q1.list().map(_.uuidArr))

      val q2 = ArrayTestTable.where(5L.bind <= _.longArr.all).sortBy(_.id).map(t => t)
      println(s"'all' sql = ${q2.selectStatement}")
      assertEquals(List(testRec2, testRec3).map(_.longArr), q2.list().map(_.longArr))

      /* notes: use 'Array("str3").bind' instead of 'Array("str3")' */
//      val q3 = ArrayTestTable.where(_.strArr @> Array("str3")).sortBy(_.id).map(t => t)
      val q3 = ArrayTestTable.where(_.strArr @> List("str3").bind).sortBy(_.id).map(t => t)
      println(s"'@>' sql = ${q3.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3).map(_.strArr.orNull), q3.list().map(_.strArr.orNull))

      val q31 = ArrayTestTable.where(List("str3").bind <@: _.strArr).sortBy(_.id).map(t => t)
      println(s"'<@' sql = ${q31.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3).map(_.strArr.orNull), q31.list().map(_.strArr.orNull))

      val q4 = ArrayTestTable.where(_.longArr @& List(5L, 17L).bind).sortBy(_.id).map(t => t)
      println(s"'&&' sql = ${q4.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3).map(_.longArr), q4.list().map(_.longArr))

      val q5 = ArrayTestTable.where(_.longArr.length() > 3.bind).sortBy(_.id).map(t => t)
      println(s"'length' sql = ${q5.selectStatement}")
      assertEquals(List(testRec1).map(_.longArr), q5.list().map(_.longArr))

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
