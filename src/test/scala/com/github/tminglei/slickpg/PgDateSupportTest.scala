package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import java.sql.{Timestamp, Time, Date}
import java.util.Calendar
import java.text.SimpleDateFormat
import scala.slick.jdbc.StaticQuery
import scala.util.Try

class PgDateSupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=postgres", driver = "org.postgresql.Driver")

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val timeFormat = new SimpleDateFormat("HH:mm:ss")
  val tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def date(str: String) = new Date(dateFormat.parse(str).getTime)
  def time(str: String) = new Time(timeFormat.parse(str).getTime)
  def ts(str: String) = new Timestamp(tsFormat.parse(str).getTime)
  def tstz(str: String) = PgDateSupportUtils.parseCalendar(str)

  case class DatetimeBean(
    id: Long,
    date: Date,
    time: Time,
    timestamp: Timestamp,
    timestamptz: Calendar,
    interval: Interval
    )

  class DatetimeTable(tag: Tag) extends Table[DatetimeBean](tag, "DatetimeTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def date = column[Date]("date")
    def time = column[Time]("time", O.Default(PgDateSupportTest.this.time("00:00:00")))
    def timestamp = column[Timestamp]("timestamp")
    def timestamptz = column[Calendar]("timestamptz")
    def interval = column[Interval]("interval")

    def * = (id, date, time, timestamp, timestamptz, interval) <> (DatetimeBean.tupled, DatetimeBean.unapply)
  }
  val Datetimes = TableQuery[DatetimeTable]

  //------------------------------------------------------------------------------

  val testRec1 = new DatetimeBean(101L, date("2010-11-3"), time("12:33:01"),
    ts("2001-1-3 13:21:00"), tstz("2001-01-03 13:21:00+08:00"), Interval("1 days 1 hours"))
  val testRec2 = new DatetimeBean(102L, date("2011-3-2"), time("3:14:7"),
    ts("2012-5-8 11:31:06"), tstz("2012-05-08 11:31:06-05:00"), Interval("1 years 36 mons 127 days"))
  val testRec3 = new DatetimeBean(103L, date("2000-5-19"), time("11:13:34"),
    ts("2019-11-3 13:19:03"), tstz("2019-11-03 13:19:03+03:00"), Interval("63 hours 16 mins 2 secs"))

  @Test
  def testDatetimeFunctions(): Unit = {
    db withSession { implicit session: Session =>
      (StaticQuery.u + "SET TIMEZONE TO '+8';").execute
      Datetimes forceInsertAll (testRec1, testRec2, testRec3)

      // basic test
//      assertEquals(List(testRec1, testRec2, testRec3), Datetimes.list)

      val q0 = Datetimes.filter(_.date === date("2010-11-3")).map(_.date)
      assertEquals(testRec1.date, q0.first)
      val q01 = Datetimes.filter(_.time === time("12:33:01")).map(_.time)
      assertEquals(testRec1.time, q01.first)
      val q02 = Datetimes.filter(_.timestamp === ts("2001-1-3 13:21:00")).map(_.timestamp)
      assertEquals(testRec1.timestamp, q02.first)
      val q03 = Datetimes.filter(_.timestamptz === tstz("2012-05-08 11:31:06-05:00")).map(_.timestamptz)
      assertEquals(testRec2.timestamptz.getTimeInMillis, q03.first.getTimeInMillis)
      val q04 = Datetimes.filter(_.interval === Interval("1 days 1 hours")).map(_.interval)
      assertEquals(testRec1.interval, q04.first)

      // datetime - '+'/'-'
      val q1 = Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time)
      println(s"[date] '+' sql = ${q1.selectStatement}")
      assertEquals(ts("2010-11-3 12:33:01"), q1.first)

      val q101 = Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date)
      println(s"[date] '+' sql = ${q101.selectStatement}")
      assertEquals(ts("2010-11-3 12:33:01"), q101.first)

      val q2 = Datetimes.filter(_.id === 101L.bind).map(r => r.date +++ r.interval)
      println(s"[date] '+++' sql = ${q2.selectStatement}")
      assertEquals(ts("2010-11-4 01:00:00"), q2.first)

      val q3 = Datetimes.filter(_.id === 101L.bind).map(r => r.time +++ r.interval)
      println(s"[date] '+++' sql = ${q3.selectStatement}")
      assertEquals(time("13:33:01"), q3.first)

      val q4 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp +++ r.interval)
      println(s"[date] '+++' sql = ${q4.selectStatement}")
      assertEquals(ts("2001-1-4 14:21:00"), q4.first)

      val q5 = Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind)
      println(s"[date] '++' sql = ${q5.selectStatement}")
      assertEquals(date("2010-11-10"), q5.first)

      val q6 = Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind)
      println(s"[date] '--' sql = ${q6.selectStatement}")
      assertEquals(date("2010-11-2"), q6.first)

      val q7 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp -- r.time)
      println(s"[date] '--' sql = ${q7.selectStatement}")
      assertEquals(ts("2001-1-3 00:47:59"), q7.first)

      val q8 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp - r.date)
      println(s"[date] '-' sql = ${q8.selectStatement}")
      assertEquals(Interval("-3590 days -10 hours -39 mins"), q8.first)

      val q801 = Datetimes.filter(_.id === 101L.bind).map(r => r.date.asColumnOf[Timestamp] - r.timestamp)
      println(s"[date] '-' sql = ${q801.selectStatement}")
      assertEquals(Interval("3590 days 10 hours 39 mins"), q801.first)

      val q9 = Datetimes.filter(_.id === 101L.bind).map(r => r.date - date("2009-7-5"))
      println(s"[date] '-' sql = ${q9.selectStatement}")
      assertEquals(486, q9.first)

      val q10 = Datetimes.filter(_.id === 101L.bind).map(r => r.time - time("2:37:00").bind)
      println(s"[date] '-' sql = ${q10.selectStatement}")
      assertEquals(Interval("9 hours 56 mins 1.00 secs"), q10.first)

      val q11 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp --- r.interval)
      println(s"[date] '---' sql = ${q11.selectStatement}")
      assertEquals(ts("2001-1-2 12:21:00"), q11.first)

      val q12 = Datetimes.filter(_.id === 101L.bind).map(r => r.time --- r.interval)
      println(s"[date] '---' sql = ${q12.selectStatement}")
      assertEquals(time("11:33:01"), q12.first)

      val q13 = Datetimes.filter(_.id === 101L.bind).map(r => r.date --- r.interval)
      println(s"[date] '---' sql = ${q13.selectStatement}")
      assertEquals(ts("2010-11-1 23:00:00"), q13.first)

      // datetime - age/part/trunc
      val q14 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.age)
      val q141 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.age(Functions.currentDate.asColumnOf[Timestamp]))
      println(s"[date] 'age' sql = ${q14.selectStatement}")
      println(s"[date] 'age' sql1 = ${q141.selectStatement}")
      assertEquals(q141.first, q14.first)

      val q15 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.part("year"))
      println(s"[date] 'part' sql = ${q15.selectStatement}")
      assertEquals(2001, q15.first, 0.00001d)

      val q1501 = Datetimes.filter(_.id === 102L.bind).map(r => r.interval.part("year"))
      println(s"[date] 'part' sql = ${q1501.selectStatement}")
      assertEquals(4, q1501.first, 0.00001d)

      val q16 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.trunc("day"))
      println(s"[date] 'trunc' sql = ${q16.selectStatement}")
      assertEquals(ts("2001-1-3 00:00:00"), q16.first)

      // interval test cases
      val q21 = Datetimes.filter(_.id === 101L.bind).map(r => r.interval + Interval("3 hours").bind)
      println(s"[date] '+' sql = ${q21.selectStatement}")
      assertEquals(Interval("1 days 4 hours"), q21.first)

      val q22 = Datetimes.filter(_.id === 101L.bind).map(r => -r.interval)
      println(s"[date] 'unary_-' sql = ${q22.selectStatement}")
      assertEquals(Interval("-1 days -1 hours"), q22.first)

      val q23 = Datetimes.filter(_.id === 101L.bind).map(r => r.interval - Interval("2 hours").bind)
      println(s"[date] '-' sql = ${q23.selectStatement}")
      assertEquals(Interval("1 days -1 hours"), q23.first)

      val q24 = Datetimes.filter(_.id === 101L.bind).map(r => r.interval * 3.5)
      println(s"[date] '*' sql = ${q24.selectStatement}")
      assertEquals(Interval("3 days 15 hours 30 mins"), q24.first)

      val q25 = Datetimes.filter(_.id === 101L.bind).map(r => r.interval / 5.0)
      println(s"[date] '*' sql = ${q25.selectStatement}")
      assertEquals(Interval("5 hours"), q25.first)

      val q26 = Datetimes.filter(_.id === 102L.bind).map(r => r.interval.justifyDays)
      println(s"[date] 'justifyDays' sql = ${q26.selectStatement}")
      assertEquals(Interval("4 years 4 mons 7 days"), q26.first)

      val q27 = Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyHours)
      println(s"[date] 'justifyHours' sql = ${q27.selectStatement}")
      assertEquals(Interval("2 days 15 hours 16 mins 2 secs"), q27.first)

      val q28 = Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyInterval)
      println(s"[date] 'justifyInterval' sql = ${q28.selectStatement}")
      assertEquals(Interval("2 days 15 hours 16 mins 2 secs"), q28.first)
      
      // timestamp with time zone cases
      val q34 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.age)
      val q341 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.age(Functions.currentDate.asColumnOf[Calendar]))
      println(s"[date] 'age' sql = ${q34.selectStatement}")
      println(s"[date] 'age' sql1 = ${q341.selectStatement}")
      assertEquals(q341.first, q34.first)

      val q35 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.part("year"))
      println(s"[date] 'part' sql = ${q35.selectStatement}")
      assertEquals(2001, q35.first, 0.00001d)

      val q36 = Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.trunc("day"))
      println(s"[date] 'trunc' sql = ${q36.selectStatement}")
      assertEquals(tstz("2001-1-3 00:00:00+8:00"), q36.first)
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      Try { Datetimes.ddl drop }
      Try { Datetimes.ddl.createStatements.foreach(s => println(s"[date] $s")) }
      Try { Datetimes.ddl create }
    }
  }
}
