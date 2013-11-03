package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import java.util.UUID

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

      val q7 = ArrayTestTable.where(_.id === 33L.bind).map(_.intArr ++ List(105, 107).bind)
      println(s"concatenate1 sql = ${q7.selectStatement}")
      assertEquals(List(101, 102, 103, 105, 107), q7.first())

      val q8 = ArrayTestTable.where(_.id === 33L.bind).map(List(105, 107).bind ++ _.intArr)
      println(s"concatenate2 sql = ${q8.selectStatement}")
      assertEquals(List(105, 107, 101, 102, 103), q8.first())

      val q9 = ArrayTestTable.where(_.id === 33L.bind).map(_.intArr + 105.bind)
      println(s"concatenate3 sql = ${q9.selectStatement}")
      assertEquals(List(101, 102, 103, 105), q9.first())

      val q10 = ArrayTestTable.where(_.id === 33L.bind).map(105.bind +: _.intArr)
      println(s"concatenate4 sql = ${q10.selectStatement}")
      assertEquals(List(105, 101, 102, 103), q10.first())

      // test array type mapper's 'updateObject' method
      ArrayTestTable.where(_.id === 33L.bind).map(r => r).mutate({ m =>
        m.row = m.row.copy( longArr = List(3, 5, 9))
      })
      val q11 = ArrayTestTable.where(_.id === 33L.bind).map(r => r.longArr)
      assertEquals(List(3,5,9), q11.first())
    }
  }

  ////////////////////////////////////////////////////////////////////////

  /**
   * disable it, since postgres jdbc didn't support uuid array now
   **/
//  case class ArrayBean1(
//    id: Long,
//    uuidArr: List[UUID]
//    )
//
//  object ArrayTestTable1 extends Table[ArrayBean1](Some("test"), "ArrayTest1") {
//    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
//    def uuidArr = column[List[UUID]]("uuidArray")
//
//    def * = id ~ uuidArr <> (ArrayBean1, ArrayBean1 unapply _)
//  }
//
//  //------------------------------------------------------------------------------
//
//  val uuid1 = UUID.randomUUID()
//  val uuid2 = UUID.randomUUID()
//  val uuid3 = UUID.randomUUID()
//
//  val rec1 = ArrayBean1(51L, List(uuid1, uuid2))
//  val rec2 = ArrayBean1(52L, List(uuid1, uuid2, uuid3))
//  val rec3 = ArrayBean1(53L, List(uuid1, uuid3))
//
//  @Test
//  def testArrayFunctions1(): Unit = {
//    db withSession { implicit session: Session =>
//      ArrayTestTable1.insert(rec1)
//      ArrayTestTable1.insert(rec2)
//      ArrayTestTable1.insert(rec3)
//
//      val q1 = ArrayTestTable1.where(_.uuidArr @> List(uuid2).bind).map(t => t)
//      println(s"uuid '@>' sql = ${q1.selectStatement}")
//      assertEquals(List(rec1, rec2), q1.list())
//    }
//  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      ArrayTestTable.ddl create;
//      ArrayTestTable1.ddl create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      ArrayTestTable.ddl drop;
//      ArrayTestTable1.ddl drop
    }
  }
}
