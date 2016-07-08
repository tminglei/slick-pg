package com.github.tminglei.slickpg

import java.sql.{Timestamp, Time, Date}
import java.util.Calendar
import java.text.SimpleDateFormat

import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration._

class PgDateSupportSuite extends FunSuite {
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

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
    def time = column[Time]("time", O.Default(PgDateSupportSuite.this.time("00:00:00")))
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

  test("Date Lifted support") {
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
                assert(b1.date === b2.date)
                assert(b1.time === b2.time)
                assert(b1.timestamp === b2.timestamp)
              }
            }
          ),
          // +
          Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time).result.head.map(
            r => assert(ts("2010-11-3 12:33:01.000") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date).result.head.map(
            r => assert(ts("2010-11-3 12:33:01.000") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date +++ r.interval).result.head.map(
            r => assert(ts("2010-11-4 01:00:00.000") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time +++ r.interval).result.head.map(
            r => assert(time("13:33:01") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp +++ r.interval).result.head.map(
            r => assert(ts("2001-1-4 14:21:00.103") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind).result.head.map(
            r => assert(date("2010-11-10") === r)
          ),
          // -
          Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind).result.head.map(
            r => assert(date("2010-11-2") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp -- r.time).result.head.map(
            r => assert(ts("2001-1-3 00:47:59.103") === r)
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
            r => assert(ts("2001-1-2 12:21:00.103") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time --- r.interval).result.head.map(
            r => assert(time("11:33:01") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date --- r.interval).result.head.map(
            r => assert(ts("2010-11-1 23:00:00.0") === r)
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
            r => assert(ts("2001-1-3 00:00:00.0") === r)
          ),
          // isFinite
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamp.isFinite).result.head.map(
            r => assert(true === r)
          ),
          // at time zone
          Datetimes.filter(_.id === 101L.bind).map(r => r.timestamptz.atTimeZone("MST")).result.head.map(
            r => assert(r.isInstanceOf[Timestamp])
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time.atTimeZone("MST")).result.head.map(
            r => assert(r.isInstanceOf[Calendar])
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
              r => assert(tstz("2001-1-3 00:00:00+8:00") === r)
            )
          )
        )
      ).andFinally(
        Datetimes.schema drop
      ).transactionally
    ), Duration.Inf)
  }
}