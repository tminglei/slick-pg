package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import java.sql.{Timestamp, Time, Date}
import java.util.Calendar
import java.text.SimpleDateFormat

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class PgDateSupportTest {
  import MyPostgresDriver.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val timeFormat = new SimpleDateFormat("HH:mm:ss")
  val tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val tsFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

  def date(str: String) = new Date(dateFormat.parse(str).getTime)
  def time(str: String) = new Time(timeFormat.parse(str).getTime)
  def ts(str: String) = new Timestamp( (if (str.contains(".")) tsFormat1 else tsFormat) .parse(str).getTime )
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
    ts("2001-1-3 13:21:00.103"), tstz("2001-01-03 13:21:00+08:00"), Interval("1 days 1 hours"))
  val testRec2 = new DatetimeBean(102L, date("2011-3-2"), time("3:14:7"),
    ts("2012-5-8 11:31:06"), tstz("2012-05-08 11:31:06-05:00"), Interval("1 years 36 mons 127 days"))
  val testRec3 = new DatetimeBean(103L, date("2000-5-19"), time("11:13:34"),
    ts("2019-11-3 13:19:03"), tstz("2019-11-03 13:19:03+03:00"), Interval("63 hours 16 mins 2 secs"))

  @Test
  def testDatetimeFunctions(): Unit = {
    Await.result(db.run(DBIO.seq(
      sqlu"SET TIMEZONE TO '+8';",
      Datetimes.schema create,
      ///
      Datetimes forceInsertAll List(testRec1, testRec2, testRec3),
      // 0. simple test
      Datetimes.to[List].result.map(
        assertEquals(List(testRec2, testRec2, testRec3), _)
      ),
      // 1. '+'
      Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time).result.head.map(
        assertEquals(ts("2010-11-3 12:33:01.000"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date).result.head.map(
        assertEquals(ts("2010-11-3 12:33:01.000"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date +++ r.interval).result.head.map(
        assertEquals(ts("2010-11-4 01:00:00.000"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time +++ r.interval).result.head.map(
        assertEquals(time("13:33:01"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp +++ r.interval).result.head.map(
        assertEquals(ts("2001-1-4 14:21:00.103"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind).result.head.map(
        assertEquals(date("2010-11-10"), _)
      ),
      // 2. '-'
      Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind).result.head.map(
        assertEquals(date("2010-11-2"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp -- r.time).result.head.map(
        assertEquals(ts("2001-1-3 00:47:59.103"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp - r.date).result.head.map(
        assertEquals(Interval("-3590 days -10 hours -38 mins -59.897 secs"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date.asColumnOf[Timestamp] - r.timestamp).result.head.map(
        assertEquals(Interval("3590 days 10 hours 38 mins 59.897 secs"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date - date("2009-7-5")).result.head.map(
        assertEquals(486, _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time - time("2:37:00").bind).result.head.map(
        assertEquals(Interval("9 hours 56 mins 1.00 secs"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp --- r.interval).result.head.map(
        assertEquals(ts("2001-1-2 12:21:00.103"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time --- r.interval).result.head.map(
        assertEquals(time("11:33:01"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date --- r.interval).result.head.map(
        assertEquals(ts("2010-11-1 23:00:00.0"), _)
      ),
      // 3. 'age'
      Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.age === r.timestamp.age(Functions.currentDate.asColumnOf[Timestamp])).result.head.map(
        assertEquals(true, _)
      ),
      // 4. 'part'
      Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.part("year")).result.head.map(
        assertEquals(2001, _, 0.00001d)
      ),
      Datetimes.filter(_.id === 102L.bind).map(r => r.interval.part("year")).result.head.map(
        assertEquals(4, _, 0.00001d)
      ),
      // 5. 'trunc'
      Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.trunc("day")).result.head.map(
        assertEquals(ts("2001-1-3 00:00:00.0"), _)
      ),
      // 6. interval
      Datetimes.filter(_.id === 101L.bind).map(r => r.interval + Interval("3 hours").bind).result.head.map(
        assertEquals(Interval("1 days 4 hours"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => -r.interval).result.head.map(
        assertEquals(Interval("-1 days -1 hours"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.interval - Interval("2 hours").bind).result.head.map(
        assertEquals(Interval("1 days -1 hours"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.interval * 3.5).result.head.map(
        assertEquals(Interval("3 days 15 hours 30 mins"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.interval / 5.0).result.head.map(
        assertEquals(Interval("5 hours"), _)
      ),
      Datetimes.filter(_.id === 102L.bind).map(r => r.interval.justifyDays).result.head.map(
        assertEquals(Interval("4 years 4 mons 7 days"), _)
      ),
      Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyHours).result.head.map(
        assertEquals(Interval("2 days 15 hours 16 mins 2 secs"), _)
      ),
      Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyInterval).result.head.map(
        assertEquals(Interval("2 days 15 hours 16 mins 2 secs"), _)
      ),
      // 7. timestamp with time zone
      Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.age === r.timestamptz.age(Functions.currentDate.asColumnOf[Calendar])).result.head.map(
        assertEquals(true, _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.part("year")).result.head.map(
        assertEquals(2001, _, 0.00001d)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.trunc("day")).result.head.map(
        assertEquals(tstz("2001-1-3 00:00:00+8:00"), _)
      ),
      ///
      Datetimes.schema drop
    ).transactionally), Duration.Inf)
  }
}