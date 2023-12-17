package com.github.tminglei.slickpg

import java.sql.{Date, Time, Timestamp}
import java.util.{Calendar, TimeZone}
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.Await
import scala.concurrent.duration._

class PgDateSupportSuite extends AnyFunSuite with PostgresContainer {

  import MyPostgresProfile.api._

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val timeFormat = new SimpleDateFormat("HH:mm:ss")
  val tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val tsFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

  def date(str: String) = new Date(dateFormat.parse(str).getTime)
  def time(str: String) = new Time(timeFormat.parse(str).getTime)
  def ts(str: String) = new Timestamp( (if (str.contains(".")) tsFormat1 else tsFormat) .parse(str).getTime )
  def tstz(s: String): Calendar = {
    val dt = OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    new Calendar.Builder()
      .setDate(dt.getYear, dt.getMonthValue -1, dt.getDayOfMonth)
      .setTimeOfDay(dt.getHour, dt.getMinute, dt.getSecond, dt.getNano / 1000000)
      .setTimeZone(TimeZone.getTimeZone(dt.getOffset))
      .build()
  }

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
    def time = column[Time]("time", O.Default(PgDateSupportSuite.this.time("00:00:00")))
    def timestamp = column[Timestamp]("timestamp")
    def timestamptz = column[Calendar]("timestamptz")
    def interval = column[Interval]("interval")

    def * = (id, date, time, timestamp, timestamptz, interval) <> ((DatetimeBean.apply _).tupled, DatetimeBean.unapply)
  }
  val Datetimes = TableQuery[DatetimeTable]

  //------------------------------------------------------------------------------

  val testRec1 = new DatetimeBean(101L, date("2010-11-3"), time("12:33:01"),
    ts("2001-1-3 13:21:00.103"), tstz("2001-01-03T13:21:00+08:00"), Interval("1 days 1 hours"))
  val testRec2 = new DatetimeBean(102L, date("2011-3-2"), time("3:14:7"),
    ts("2012-5-8 11:31:06"), tstz("2012-05-08T11:31:06-05:00"), Interval("1 years 36 mons 127 days"))
  val testRec3 = new DatetimeBean(103L, date("2000-5-19"), time("11:13:34"),
    ts("2019-11-3 13:19:03"), tstz("2019-11-03T13:19:03+03:00"), Interval("63 hours 16 mins 2 secs"))

  test("Date Lifted support") {
    def when[A](cond: Boolean)(a: => A): Option[A] =
      if (cond) Some(a) else None

    Await.result(db.run(
      DBIO.seq(
        sqlu"SET TIMEZONE TO '+8';",
        Datetimes.schema create,
        ///
        Datetimes forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          Datetimes.to[List].result.map(
            r => List(testRec1, testRec2, testRec3).zip(r).map {
              case (b1, b2) => {
                assert(b1.date.toLocalDate === b2.date.toLocalDate)
                assert(b1.time.toLocalTime === b2.time.toLocalTime)
                assert(b1.timestamp.toLocalDateTime === b2.timestamp.toLocalDateTime)
              }
            }
          ),
          // +
          Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time).result.head.map(
            r => assert(ts("2010-11-3 12:33:01.000").toLocalDateTime === r.toLocalDateTime)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date).result.head.map(
            r => assert(ts("2010-11-3 12:33:01.000").toLocalDateTime === r.toLocalDateTime)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date +++ r.interval).result.head.map(
            r => assert(ts("2010-11-4 01:00:00.000").toLocalDateTime === r.toLocalDateTime)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time +++ r.interval).result.head.map(
            r => assert(time("13:33:01").toLocalTime === r.toLocalTime)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp +++ r.interval).result.head.map(
            r => assert(ts("2001-1-4 14:21:00.103").toLocalDateTime === r.toLocalDateTime)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind).result.head.map(
            r => assert(date("2010-11-10").toLocalDate === r.toLocalDate)
          ),
          // -
          Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind).result.head.map(
            r => assert(date("2010-11-2").toLocalDate === r.toLocalDate)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp -- r.time).result.head.map(
            r => assert(ts("2001-1-3 00:47:59.103").toLocalDateTime === r.toLocalDateTime)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp - r.date).result.head.map(
            r => assert(Interval("-3590 days -10 hours -38 mins -59.897 secs") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date.asColumnOf[Timestamp] - r.timestamp).result.head.map(
            r => assert(Interval("3590 days 10 hours 38 mins 59.897 secs") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date - date("2009-7-5")).result.head.map(
            r => assert(486 === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time - time("2:37:00").bind).result.head.map(
            r => assert(Interval("9 hours 56 mins 1.00 secs") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp --- r.interval).result.head.map(
            r => assert(ts("2001-1-2 12:21:00.103").toLocalDateTime === r.toLocalDateTime)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time --- r.interval).result.head.map(
            r => assert(time("11:33:01").toLocalTime === r.toLocalTime)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date --- r.interval).result.head.map(
            r => assert(ts("2010-11-1 23:00:00.0").toLocalDateTime === r.toLocalDateTime)
          ),
          // age
//          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.age === r.timestamp.age(Functions.currentDate.asColumnOf[Timestamp])).result.head.map(
//            r => assert(true === r)
//          ),
          // part
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.part("year")).result.head.map(
            r => assert(Math.abs(2001 - r) < 0.00001d)
          ),
          Datetimes.filter(_.id === 102L.bind).map(r => r.interval.part("year")).result.head.map(
            r => assert(Math.abs(4 - r) < 0.00001d)
          ),
          // trunc
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.trunc("day")).result.head.map(
            r => assert(ts("2001-1-3 00:00:00.0").toLocalDateTime === r.toLocalDateTime)
          ),
          // dateBin
          DBIO.seq(when(pgVersion.take(2).toInt >= 14)(
            Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.dateBin("1 hour", ts("2001-1-3 18:35:17"))).result.head.map(
              r => assert(ts("2001-01-03 12:35:17").toLocalDateTime === r.toLocalDateTime)
            )).toSeq: _*),
          // isFinite
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.isFinite).result.head.map(
            r => assert(true === r)
          ),
          // at time zone
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.atTimeZone("MST")).result.head.map(
            r => assert(r.isInstanceOf[Calendar])
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time.atTimeZone("MST")).result.head.map(
            r => assert(r.isInstanceOf[Time])
          ),
          // interval
          DBIO.seq(
            // +
            Datetimes.filter(_.id === 101L.bind).map(r => r.interval + Interval("3 hours").bind).result.head.map(
              r => assert(Interval("1 days 4 hours") === r)
            ),
            // -x
            Datetimes.filter(_.id === 101L.bind).map(r => -r.interval).result.head.map(
              r => assert(Interval("-1 days -1 hours") === r)
            ),
            // -
            Datetimes.filter(_.id === 101L.bind).map(r => r.interval - Interval("2 hours").bind).result.head.map(
              r => assert(Interval("1 days -1 hours") === r)
            ),
            // *
            Datetimes.filter(_.id === 101L.bind).map(r => r.interval * 3.5).result.head.map(
              r => assert(Interval("3 days 15 hours 30 mins") === r)
            ),
            // /
            Datetimes.filter(_.id === 101L.bind).map(r => r.interval / 5.0).result.head.map(
              r => assert(Interval("5 hours") === r)
            ),
            // justifyDays
            Datetimes.filter(_.id === 102L.bind).map(r => r.interval.justifyDays).result.head.map(
              r => assert(Interval("4 years 4 mons 7 days") === r)
            ),
            // justifyHours
            Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyHours).result.head.map(
              r => assert(Interval("2 days 15 hours 16 mins 2 secs") === r)
            ),
            // justifyInterval
            Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyInterval).result.head.map(
              r => assert(Interval("2 days 15 hours 16 mins 2 secs") === r)
            )
          ),
          // timestamp with time zone
          DBIO.seq(
            // age
//            Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.age === r.timestamptz.age(Functions.currentDate.asColumnOf[Calendar])).result.head.map(
//              r => assert(true === r)
//            ),
            // part
            Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.part("year")).result.head.map(
              r => assert(Math.abs(2001 - r) < 0.00001d)
            ),
            // trunc
            Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.trunc("day")).result.head.map(
              r => assert(tstz("2001-01-03T00:00:00+08:00").getTimeInMillis === r.getTimeInMillis)
            ),
            // dateBin
            DBIO.seq(when(pgVersion.take(2).toInt >= 14)(
              Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.dateBin("1 hour", tstz("2001-01-02T18:35:17+08:00"))).result.head.map(
                r => assert(tstz("2001-01-03T12:35:17+08:00").getTimeInMillis === r.getTimeInMillis)
              )).toSeq: _*)
          )
        )
      ).andFinally(
        Datetimes.schema drop
      ).transactionally
    ), Duration.Inf)
  }
}