package org.slick.driver
package pg

import org.junit._
import org.junit.Assert._
import java.sql.Timestamp
import org.slick.Range

class PgRangeSupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  val tsFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def timestamp(str: String) = new Timestamp(tsFormatter.parse(str).getTime)

  case class RangeBean(
    id: Long,
    intRange: Range[Int],
    floatRange: Range[Float],
    tsRange: Option[Range[Timestamp]]
    )

  object RangeTestTable extends Table[RangeBean](Some("test"), "RangeTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def intRange = column[Range[Int]]("intRange", O.DBType("int4range"))
    def floatRange = column[Range[Float]]("floatRange", O.DBType("numrange"))
    def tsRange = column[Option[Range[Timestamp]]]("tsRange", O.DBType("tsrange"))

    def * = id ~ intRange ~ floatRange ~ tsRange <> (RangeBean, RangeBean unapply _)
  }

  //-------------------------------------------------------------------------------

  val testRec1 = RangeBean(33L, Range(3, 5), Range(1.5f, 3.3f),
    Some(Range(timestamp("2010-01-01 14:30:00"), timestamp("2010-01-03 15:30:00"))))
  val testRec2 = RangeBean(35L, Range(31, 59), Range(11.5f, 33.3f),
    Some(Range(timestamp("2011-01-01 14:30:00"), timestamp("2011-11-01 15:30:00"))))
  val testRec3 = RangeBean(41L, Range(1, 5), Range(7.5f, 15.3f), None)

  @Test
  def testSimpleInsertFetch(): Unit = {
    db withSession { implicit session: Session =>
      RangeTestTable.insert(testRec1)

      val rec1 = RangeTestTable.where(_.id === testRec1.id.bind).map(t => t).first()
      assertEquals(testRec1, rec1)
    }
  }

  @Test
  def testRangeFunctions(): Unit = {
    db withSession { implicit session: Session =>
      RangeTestTable.insert(testRec1)
      RangeTestTable.insert(testRec2)
      RangeTestTable.insert(testRec3)


      val q0 = RangeTestTable.where(_.tsRange @>^ timestamp("2011-10-01 15:30:00").bind).sortBy(_.id).map(t => t)
      println(s"'@>^' sql = ${q0.selectStatement}")
      assertEquals(List(testRec2), q0.list())

      val q01 = RangeTestTable.where(timestamp("2011-10-01 15:30:00").bind <@^: _.tsRange).sortBy(_.id).map(t => t)
      println(s"'<@^' sql = ${q01.selectStatement}")
      assertEquals(List(testRec2), q01.list())

      val q1 = RangeTestTable.where(_.floatRange @> Range(10.5f, 12f).bind).sortBy(_.id).map(t => t)
      println(s"'@>' sql = ${q1.selectStatement}")
      assertEquals(List(testRec3), q1.list())

      val q11 = RangeTestTable.where(Range(10.5f, 12f).bind <@: _.floatRange).sortBy(_.id).map(t => t)
      println(s"'<@' sql = ${q11.selectStatement}")
      assertEquals(List(testRec3), q11.list())

      val q2 = RangeTestTable.where(_.intRange @& Range(4,6).bind).sortBy(_.id).map(t => t)
      println(s"'@&' sql = ${q2.selectStatement}")
      assertEquals(List(testRec1, testRec3), q2.list())

      val q3 = RangeTestTable.where(_.intRange << Range(10, 15).bind).sortBy(_.id).map(t => t)
      println(s"'<<' sql = ${q3.selectStatement}")
      assertEquals(List(testRec1, testRec3), q3.list())

      val q4 = RangeTestTable.where(_.intRange >> Range(10, 15).bind).sortBy(_.id).map(t => t)
      println(s"'<<' sql = ${q4.selectStatement}")
      assertEquals(List(testRec2), q4.list())

      val q5 = RangeTestTable.where(_.floatRange &< Range(2.9f, 7.7f).bind).sortBy(_.id).map(t => t)
      println(s"'&<' sql = ${q5.selectStatement}")
      assertEquals(List(testRec1), q5.list())

      val q6 = RangeTestTable.where(_.floatRange &> Range(2.9f, 7.7f).bind).sortBy(_.id).map(t => t)
      println(s"'&>' sql = ${q6.selectStatement}")
      assertEquals(List(testRec2, testRec3), q6.list())

      val q7 = RangeTestTable.where(_.intRange -|- Range(5, 31).bind).sortBy(_.id).map(t => t)
      println(s"'-|-' sql = ${q7.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q7.list())

      val q8 = RangeTestTable.where(_.id === 41L).map(t => t.intRange + Range(3, 6).bind)
      println(s"'+' sql = ${q8.selectStatement}")
      assertEquals(Range(1, 6), q8.first())

      val q9 = RangeTestTable.where(_.id === 41L).map(t => t.intRange * Range(3, 6).bind)
      println(s"'*' sql = ${q9.selectStatement}")
      assertEquals(Range(3, 5), q9.first())

      val q10 = RangeTestTable.where(_.id === 41L).map(t => t.intRange - Range(3, 6).bind)
      println(s"'-' sql = ${q10.selectStatement}")
      assertEquals(Range(1, 3), q10.first())
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      RangeTestTable.ddl create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      RangeTestTable.ddl drop
    }
  }
}
