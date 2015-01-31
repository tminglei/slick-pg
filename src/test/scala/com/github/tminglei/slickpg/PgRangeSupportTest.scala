package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import java.sql.Timestamp
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.util.Try

class PgRangeSupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  val tsFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def ts(str: String) = new Timestamp(tsFormatter.parse(str).getTime)

  case class RangeBean(
    id: Long,
    intRange: Range[Int],
    floatRange: Range[Float],
    tsRange: Option[Range[Timestamp]]
    )

  class RangeTestTable(tag: Tag) extends Table[RangeBean](tag, "RangeTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def intRange = column[Range[Int]]("int_range", O.Default(Range(3, 5)))
    def floatRange = column[Range[Float]]("float_range")
    def tsRange = column[Option[Range[Timestamp]]]("ts_range")

    def * = (id, intRange, floatRange, tsRange) <> (RangeBean.tupled, RangeBean.unapply)
  }
  val RangeTests = TableQuery[RangeTestTable]

  //-------------------------------------------------------------------------------

  val testRec1 = RangeBean(33L, Range(3, 5), Range(1.5f, 3.3f),
    Some(Range(ts("2010-01-01 14:30:00"), ts("2010-01-03 15:30:00"))))
  val testRec2 = RangeBean(35L, Range(31, 59), Range(11.5f, 33.3f),
    Some(Range(ts("2011-01-01 14:30:00"), ts("2011-11-01 15:30:00"))))
  val testRec3 = RangeBean(41L, Range(1, 5), Range(7.5f, 15.3f), None)

  @Test
  def testRangeFunctions(): Unit = {
    db withSession { implicit session: Session =>
      Try { RangeTests.ddl drop }
      Try { RangeTests.ddl.createStatements.foreach(s => println(s"[range] $s")) }
      Try { RangeTests.ddl create }

      RangeTests.forceInsertAll(testRec1, testRec2, testRec3)

      val q0 = RangeTests.filter(_.tsRange @>^ ts("2011-10-01 15:30:00")).sortBy(_.id).map(t => t)
      println(s"[range] '@>^' sql = ${q0.selectStatement}")
      assertEquals(List(testRec2), q0.list)

      val q01 = RangeTests.filter(ts("2011-10-01 15:30:00").bind <@^: _.tsRange).sortBy(_.id).map(t => t)
      println(s"[range] '<@^' sql = ${q01.selectStatement}")
      assertEquals(List(testRec2), q01.list)

      val q1 = RangeTests.filter(_.floatRange @> Range(10.5f, 12f).bind).sortBy(_.id).map(t => t)
      println(s"[range] '@>' sql = ${q1.selectStatement}")
      assertEquals(List(testRec3), q1.list)

      val q11 = RangeTests.filter(Range(10.5f, 12f).bind <@: _.floatRange).sortBy(_.id).map(t => t)
      println(s"[range] '<@' sql = ${q11.selectStatement}")
      assertEquals(List(testRec3), q11.list)

      val q2 = RangeTests.filter(_.intRange @& Range(4,6).bind).sortBy(_.id).map(t => t)
      println(s"[range] '@&' sql = ${q2.selectStatement}")
      assertEquals(List(testRec1, testRec3), q2.list)

      val q3 = RangeTests.filter(_.intRange << Range(10, 15).bind).sortBy(_.id).map(t => t)
      println(s"[range] '<<' sql = ${q3.selectStatement}")
      assertEquals(List(testRec1, testRec3), q3.list)

      val q4 = RangeTests.filter(_.intRange >> Range(10, 15).bind).sortBy(_.id).map(t => t)
      println(s"[range] '<<' sql = ${q4.selectStatement}")
      assertEquals(List(testRec2), q4.list)

      val q5 = RangeTests.filter(_.floatRange &< Range(2.9f, 7.7f).bind).sortBy(_.id).map(t => t)
      println(s"[range] '&<' sql = ${q5.selectStatement}")
      assertEquals(List(testRec1), q5.list)

      val q6 = RangeTests.filter(_.floatRange &> Range(2.9f, 7.7f).bind).sortBy(_.id).map(t => t)
      println(s"[range] '&>' sql = ${q6.selectStatement}")
      assertEquals(List(testRec2, testRec3), q6.list)

      val q7 = RangeTests.filter(_.intRange -|- Range(5, 31).bind).sortBy(_.id).map(t => t)
      println(s"[range] '-|-' sql = ${q7.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q7.list)

      val q8 = RangeTests.filter(_.id === 41L).map(t => t.intRange + Range(3, 6).bind)
      println(s"[range] '+' sql = ${q8.selectStatement}")
      assertEquals(Range(1, 6), q8.first)

      val q9 = RangeTests.filter(_.id === 41L).map(t => t.intRange * Range(3, 6).bind)
      println(s"[range] '*' sql = ${q9.selectStatement}")
      assertEquals(Range(3, 5), q9.first)

      val q10 = RangeTests.filter(_.id === 41L).map(t => t.intRange - Range(3, 6).bind)
      println(s"[range] '-' sql = ${q10.selectStatement}")
      assertEquals(Range(1, 3), q10.first)

      val q12 = RangeTests.filter(_.id === 41L).map(t => t.intRange.lower)
      println(s"[range] 'lower' sql = ${q12.selectStatement}")
      assertEquals(1, q12.first)

      val q13 = RangeTests.filter(_.id === 41L).map(t => t.intRange.upper)
      println(s"[range] 'upper' sql = ${q13.selectStatement}")
      assertEquals(5, q13.first)
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Test
  def testPlainRangeFunctions(): Unit = {
    import MyPlainPostgresDriver.plainImplicits._

    implicit val getRangeBeanResult = GetResult(r =>
      RangeBean(r.nextLong(), r.nextIntRange(), r.nextFloatRange(), r.nextTimestampRangeOption()))

    db withSession { implicit session: Session =>
      Try { Q.updateNA("drop table if exists RangeTest cascade").execute }
      Try {
        Q.updateNA("create table RangeTest(" +
          "id int8 not null primary key, " +
          "int_range int4range not null, " +
          "float_range numrange not null, " +
          "ts_range tsrange)"
        ).execute
      }

      val rBean = RangeBean(33L, Range(3, 5), Range(1.5f, 3.3f),
        Some(Range(ts("2010-01-01 14:30:00"), ts("2010-01-03 15:30:00"))))

      (Q.u + "insert into RangeTest values(" +? rBean.id + ", " +?rBean.intRange + ", " +? rBean.floatRange + ", " +? rBean.tsRange + ")").execute

      val found = (Q[RangeBean] + "select * from RangeTest where id = " +? rBean.id).first

      assertEquals(rBean, found)
    }
  }
}
