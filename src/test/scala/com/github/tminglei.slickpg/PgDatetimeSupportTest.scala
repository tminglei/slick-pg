package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import java.sql.{Timestamp, Time, Date}
import java.text.SimpleDateFormat

class PgDatetimeSupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val timeFormat = new SimpleDateFormat("HH:mm:ss")
  val tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def date(str: String) = new Date(dateFormat.parse(str).getTime)
  def time(str: String) = new Time(timeFormat.parse(str).getTime)
  def ts(str: String) = new Timestamp(tsFormat.parse(str).getTime)

  case class DatetimeBean(
    id: Long,
    date: Date,
    time: Time,
    timestamp: Timestamp,
    interval: Interval
    )

  object DatetimeTable extends Table[DatetimeBean]("DatetimeTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def date = column[Date]("date")
    def time = column[Time]("time")
    def timestamp = column[Timestamp]("timestamp")
    def interval = column[Interval]("interval")

    def * = id ~ date ~ time ~ timestamp ~ interval <> (DatetimeBean, DatetimeBean.unapply _)
  }

  //------------------------------------------------------------------------------

  val testRec1 = new DatetimeBean(101L, date("2010-11-3"), time("12:33:01"), ts("2001-1-3 13:21:00"), Interval("1 days 1 hours"))
  val testRec2 = new DatetimeBean(102L, date("2011-3-2"), time("3:14:7"), ts("2012-5-8 11:31:06"), Interval("1 years 36 mons 127 days"))
  val testRec3 = new DatetimeBean(103L, date("2000-5-19"), time("11:13:34"), ts("2019-11-3 13:19:03"), Interval("63 hours 16 mins 2 secs"))

  @Test
  def testDatetimeFunctions(): Unit = {
    db withSession { implicit session: Session =>
      DatetimeTable.insert(testRec1)
      DatetimeTable.insert(testRec2)
      DatetimeTable.insert(testRec3)

      // datetime - '+'/'-'
      val q1 = DatetimeTable.where(_.id === 101L.bind).map(r => r.date + r.time)
      println(s"'+' sql = ${q1.selectStatement}")
      assertEquals(ts("2010-11-3 12:33:01"), q1.first())

      val q101 = DatetimeTable.where(_.id === 101L.bind).map(r => r.time + r.date)
      println(s"'+' sql = ${q101.selectStatement}")
      assertEquals(ts("2010-11-3 12:33:01"), q101.first())

      val q2 = DatetimeTable.where(_.id === 101L.bind).map(r => r.date +++ r.interval)
      println(s"'+++' sql = ${q2.selectStatement}")
      assertEquals(ts("2010-11-4 01:00:00"), q2.first())

      val q3 = DatetimeTable.where(_.id === 101L.bind).map(r => r.time +++ r.interval)
      println(s"'+++' sql = ${q3.selectStatement}")
      assertEquals(time("13:33:01"), q3.first())

      val q4 = DatetimeTable.where(_.id === 101L.bind).map(r => r.timestamp +++ r.interval)
      println(s"'+++' sql = ${q4.selectStatement}")
      assertEquals(ts("2001-1-4 14:21:00"), q4.first())

      val q5 = DatetimeTable.where(_.id === 101L.bind).map(r => r.date ++ 7.bind)
      println(s"'++' sql = ${q5.selectStatement}")
      assertEquals(date("2010-11-10"), q5.first())

      val q6 = DatetimeTable.where(_.id === 101L.bind).map(r => r.date -- 1.bind)
      println(s"'--' sql = ${q6.selectStatement}")
      assertEquals(date("2010-11-2"), q6.first())

      val q7 = DatetimeTable.where(_.id === 101L.bind).map(r => r.timestamp -- r.time)
      println(s"'--' sql = ${q7.selectStatement}")
      assertEquals(ts("2001-1-3 00:47:59"), q7.first())

      val q8 = DatetimeTable.where(_.id === 101L.bind).map(r => r.timestamp - r.date)
      println(s"'-' sql = ${q8.selectStatement}")
      assertEquals(Interval("-3590 days -10 hours -39 mins"), q8.first())

      val q801 = DatetimeTable.where(_.id === 101L.bind).map(r => r.date.asColumnOf[Timestamp] - r.timestamp)
      println(s"'-' sql = ${q801.selectStatement}")
      assertEquals(Interval("3590 days 10 hours 39 mins"), q801.first())

      val q9 = DatetimeTable.where(_.id === 101L.bind).map(r => r.date - date("2009-7-5"))
      println(s"'-' sql = ${q9.selectStatement}")
      assertEquals(486, q9.first())

      val q10 = DatetimeTable.where(_.id === 101L.bind).map(r => r.time - time("2:37:00").bind)
      println(s"'-' sql = ${q10.selectStatement}")
      assertEquals(Interval("9 hours 56 mins 1.00 secs"), q10.first())

      val q11 = DatetimeTable.where(_.id === 101L.bind).map(r => r.timestamp --- r.interval)
      println(s"'---' sql = ${q11.selectStatement}")
      assertEquals(ts("2001-1-2 12:21:00"), q11.first())

      val q12 = DatetimeTable.where(_.id === 101L.bind).map(r => r.time --- r.interval)
      println(s"'---' sql = ${q12.selectStatement}")
      assertEquals(time("11:33:01"), q12.first())

      val q13 = DatetimeTable.where(_.id === 101L.bind).map(r => r.date --- r.interval)
      println(s"'---' sql = ${q13.selectStatement}")
      assertEquals(ts("2010-11-1 23:00:00"), q13.first())

      // datetime - age/part/trunc
      val q14 = DatetimeTable.where(_.id === 101L.bind).map(r => r.timestamp.age)
      val q141 = DatetimeTable.where(_.id === 101L.bind).map(r => r.timestamp.age(Functions.currentDate.asColumnOf[Timestamp]))
      println(s"'age' sql = ${q14.selectStatement}")
      println(s"'age' sql1 = ${q141.selectStatement}")
      assertEquals(q141.first(), q14.first())

      val q15 = DatetimeTable.where(_.id === 101L.bind).map(r => r.timestamp.part("year"))
      println(s"'part' sql = ${q15.selectStatement}")
      assertEquals(2001, q15.first(), 0.00001d)

      val q1501 = DatetimeTable.where(_.id === 102L.bind).map(r => r.interval.part("year"))
      println(s"'part' sql = ${q1501.selectStatement}")
      assertEquals(4, q1501.first(), 0.00001d)

      val q16 = DatetimeTable.where(_.id === 101L.bind).map(r => r.timestamp.trunc("day"))
      println(s"'trunc' sql = ${q16.selectStatement}")
      assertEquals(ts("2001-1-3 00:00:00"), q16.first())

      // interval test cases
      val q21 = DatetimeTable.where(_.id === 101L.bind).map(r => r.interval + Interval("3 hours").bind)
      println(s"'+' sql = ${q21.selectStatement}")
      assertEquals(Interval("1 days 4 hours"), q21.first())

      val q22 = DatetimeTable.where(_.id === 101L.bind).map(r => -r.interval)
      println(s"'unary_-' sql = ${q22.selectStatement}")
      assertEquals(Interval("-1 days -1 hours"), q22.first())

      val q23 = DatetimeTable.where(_.id === 101L.bind).map(r => r.interval - Interval("2 hours").bind)
      println(s"'-' sql = ${q23.selectStatement}")
      assertEquals(Interval("1 days -1 hours"), q23.first())

      val q24 = DatetimeTable.where(_.id === 101L.bind).map(r => r.interval * 3.5)
      println(s"'*' sql = ${q24.selectStatement}")
      assertEquals(Interval("3 days 15 hours 30 mins"), q24.first())

      val q25 = DatetimeTable.where(_.id === 101L.bind).map(r => r.interval / 5.0)
      println(s"'*' sql = ${q25.selectStatement}")
      assertEquals(Interval("5 hours"), q25.first())

      val q26 = DatetimeTable.where(_.id === 102L.bind).map(r => r.interval.justifyDays)
      println(s"'justifyDays' sql = ${q26.selectStatement}")
      assertEquals(Interval("4 years 4 mons 7 days"), q26.first())

      val q27 = DatetimeTable.where(_.id === 103L.bind).map(r => r.interval.justifyHours)
      println(s"'justifyHours' sql = ${q27.selectStatement}")
      assertEquals(Interval("2 days 15 hours 16 mins 2 secs"), q27.first())

      val q28 = DatetimeTable.where(_.id === 103L.bind).map(r => r.interval.justifyInterval)
      println(s"'justifyInterval' sql = ${q28.selectStatement}")
      assertEquals(Interval("2 days 15 hours 16 mins 2 secs"), q28.first())
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      DatetimeTable.ddl create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      DatetimeTable.ddl drop
    }
  }
}
