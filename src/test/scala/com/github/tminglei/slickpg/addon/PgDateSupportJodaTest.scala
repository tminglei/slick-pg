package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import scala.slick.jdbc.{StaticQuery => Q, GetResult}
import org.joda.time._
import scala.util.Try

class PgDateSupportJodaTest {
  import scala.slick.driver.PostgresDriver

  object MyPostgresDriver extends PostgresDriver
                            with PgDateSupportJoda {

    override lazy val Implicit = new Implicits with DateTimeImplicits
    override val simple = new Implicits with SimpleQL with DateTimeImplicits

    ///
    val plainImplicits = new Implicits with JodaDateTimePlainImplicits
  }

  ///
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  case class DatetimeBean(
    id: Long,
    date: LocalDate,
    time: LocalTime,
    dateTime: LocalDateTime,
    dateTimetz: DateTime,
    interval: Period
    )

  class DatetimeTable(tag: Tag) extends Table[DatetimeBean](tag, "DatetimeJodaTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def date = column[LocalDate]("date")
    def time = column[LocalTime]("time")
    def datetime = column[LocalDateTime]("datetime")
    def datetimetz = column[DateTime]("datetimetz")
    def interval = column[Period]("interval")

    def * = (id, date, time, datetime, datetimetz, interval) <> (DatetimeBean.tupled, DatetimeBean.unapply)
  }
  val Datetimes = TableQuery[DatetimeTable]

  //------------------------------------------------------------------------------

  val testRec1 = new DatetimeBean(101L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01.101357"),
    LocalDateTime.parse("2001-01-03T13:21:00.223571"), DateTime.parse("2001-01-03 13:21:00.102203+08", jodaTzDateTimeFormatter),
    Period.parse("P1DT1H1M0.335701S"))
  val testRec2 = new DatetimeBean(102L, LocalDate.parse("2011-03-02"), LocalTime.parse("03:14:07"),
    LocalDateTime.parse("2012-05-08T11:31:06"), DateTime.parse("2012-05-08 11:31:06.113-05", jodaTzDateTimeFormatter),
    Period.parse("P1587D"))
  val testRec3 = new DatetimeBean(103L, LocalDate.parse("2000-05-19"), LocalTime.parse("11:13:34"),
    LocalDateTime.parse("2019-11-03T13:19:03"), DateTime.parse("2019-11-03 13:19:03.000+03", jodaTzDateTimeFormatter),
    Period.parse("PT63H16M2S"))

  @Test
  def testDatetimeFunctions(): Unit = {
    db withSession { implicit session: Session =>
      Try { Datetimes.ddl drop }
      Try { Datetimes.ddl create }

      (Q.u + "SET TIMEZONE TO '+8';").execute
      Datetimes forceInsertAll (testRec1, testRec2, testRec3)

      val q0 = Datetimes.map(r => r)
      // testRec2 and testRec3 will fail to equal test, because of different time zone
      assertEquals(testRec1/*List(testRec1, testRec2, testRec3)*/, q0.first)

      val q00 = Datetimes.filter(_.id === 101L.bind).map { r =>
        (r.date.isFinite, r.datetime.isFinite, r.interval.isFinite)}
      assertEquals((true, true, true), q00.first)

      // datetime - '+'/'-'
      val q1 = Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time)
      println(s"[joda] '+' sql = ${q1.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-03T12:33:01.101357"), q1.first)

      val q101 = Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date)
      println(s"[joda] '+' sql = ${q101.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-03T12:33:01.101"), q101.first)

      val q2 = Datetimes.filter(_.id === 101L.bind).map(r => r.date +++ r.interval)
      println(s"[joda] '+++' sql = ${q2.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-04T01:01:00.335"), q2.first)

      val q3 = Datetimes.filter(_.id === 101L.bind).map(r => r.time +++ r.interval)
      println(s"[joda] '+++' sql = ${q3.selectStatement}")
      assertEquals(LocalTime.parse("13:34:01.436"), q3.first)

      val q4 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime +++ r.interval)
      println(s"[joda] '+++' sql = ${q4.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-04T14:22:00.558"), q4.first)

      val q5 = Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind)
      println(s"[joda] '++' sql = ${q5.selectStatement}")
      assertEquals(LocalDate.parse("2010-11-10"), q5.first)

      val q6 = Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind)
      println(s"[joda] '--' sql = ${q6.selectStatement}")
      assertEquals(LocalDate.parse("2010-11-02"), q6.first)

      val q7 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime -- r.time)
      println(s"[joda] '--' sql = ${q7.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-03T00:47:59.122"), q7.first)

      val q8 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime - r.date)
      println(s"[joda] '-' sql = ${q8.selectStatement}")
      assertEquals(Period.parse("P-3590DT-10H-38M-60S").plus(Period.parse("PT0.222S")), q8.first)

      val q801 = Datetimes.filter(_.id === 101L.bind).map(r => r.date.asColumnOf[LocalDateTime] - r.datetime)
      println(s"[joda] '-' sql = ${q801.selectStatement}")
      assertEquals(Period.parse("P3590DT10H38M59.777S"), q801.first)

      val q9 = Datetimes.filter(_.id === 101L.bind).map(r => r.date - LocalDate.parse("2009-07-05"))
      println(s"[joda] '-' sql = ${q9.selectStatement}")
      assertEquals(486, q9.first)

      val q10 = Datetimes.filter(_.id === 101L.bind).map(r => r.time - LocalTime.parse("02:37:00").bind)
      println(s"[joda] '-' sql = ${q10.selectStatement}")
      assertEquals(Period.parse("PT9H56M1.100S"), q10.first)

      val q11 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime --- r.interval)
      println(s"[joda] '---' sql = ${q11.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-02T12:19:59.888"), q11.first)

      val q12 = Datetimes.filter(_.id === 101L.bind).map(r => r.time --- r.interval)
      println(s"[joda] '---' sql = ${q12.selectStatement}")
      assertEquals(LocalTime.parse("11:32:00.766"), q12.first)

      val q13 = Datetimes.filter(_.id === 101L.bind).map(r => r.date --- r.interval)
      println(s"[joda] '---' sql = ${q13.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-01T22:58:59.665"), q13.first)

      // datetime - age/part/trunc
      val q14 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.age)
      val q141 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.age(Functions.currentDate.asColumnOf[LocalDateTime]))
      println(s"[joda] 'age' sql = ${q14.selectStatement}")
      println(s"[joda] 'age' sql1 = ${q141.selectStatement}")
      assertEquals(q141.first, q14.first)

      val q15 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.part("year"))
      println(s"[joda] 'part' sql = ${q15.selectStatement}")
      assertEquals(2001, q15.first, 0.00001d)

      val q1501 = Datetimes.filter(_.id === 102L.bind).map(r => r.interval.part("year"))
      println(s"[joda] 'part' sql = ${q1501.selectStatement}")
      assertEquals(0, q1501.first, 0.00001d)

      val q16 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.trunc("day"))
      println(s"[joda] 'trunc' sql = ${q16.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-03T00:00:00"), q16.first)

      // interval test cases
      val q21 = Datetimes.filter(_.id === 101L.bind).map(r => r.interval + Period.parse("PT3H").bind)
      println(s"[joda] '+' sql = ${q21.selectStatement}")
      assertEquals(Period.parse("P1DT4H1M0.335S"), q21.first)

      val q22 = Datetimes.filter(_.id === 101L.bind).map(r => -r.interval)
      println(s"[joda] 'unary_-' sql = ${q22.selectStatement}")
      assertEquals(Period.parse("P-1DT-1H-1M-1S").plus(Period.parse("PT0.665S")), q22.first)

      val q23 = Datetimes.filter(_.id === 101L.bind).map(r => r.interval - Period.parse("PT2H").bind)
      println(s"[joda] '-' sql = ${q23.selectStatement}")
      assertEquals(Period.parse("P1DT-58M-60S").plus(Period.parse("PT0.335S")), q23.first)

      val q24 = Datetimes.filter(_.id === 101L.bind).map(r => r.interval * 3.5)
      println(s"[joda] '*' sql = ${q24.selectStatement}")
      assertEquals(Period.parse("P3DT15H33M31.172S"), q24.first)

      val q25 = Datetimes.filter(_.id === 101L.bind).map(r => r.interval / 5.0)
      println(s"[joda] '*' sql = ${q25.selectStatement}")
      assertEquals(Period.parse("PT5H12.067S"), q25.first)

      val q26 = Datetimes.filter(_.id === 102L.bind).map(r => r.interval.justifyDays)
      println(s"[joda] 'justifyDays' sql = ${q26.selectStatement}")
      assertEquals(Period.parse("P4Y4M27D"), q26.first)

      val q27 = Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyHours)
      println(s"[joda] 'justifyHours' sql = ${q27.selectStatement}")
      assertEquals(Period.parse("P2DT15H16M2S"), q27.first)

      val q28 = Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyInterval)
      println(s"[joda] 'justifyInterval' sql = ${q28.selectStatement}")
      assertEquals(Period.parse("P2DT15H16M2S"), q28.first)
      
      // timestamp with time zone cases
      val q34 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.age)
      val q341 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.age(Functions.currentDate.asColumnOf[DateTime]))
      println(s"[joda] 'age' sql = ${q34.selectStatement}")
      println(s"[joda] 'age' sql1 = ${q341.selectStatement}")
      assertEquals(q341.first, q34.first)

      val q35 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.part("year"))
      println(s"[joda] 'part' sql = ${q35.selectStatement}")
      assertEquals(2001, q35.first, 0.00001d)

      val q36 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.trunc("day"))
      println(s"[joda] 'trunc' sql = ${q36.selectStatement}")
      assertEquals(DateTime.parse("2001-01-03 00:00:00.000+08", jodaTzDateTimeFormatter), q36.first)

      ///update and check
      val now = LocalDateTime.now
      Datetimes.filter(_.id === 101L.bind).map(_.datetime).update(now)
      assertEquals(now, Datetimes.filter(_.id === 101L.bind).map(_.datetime).first)

      val now1 = DateTime.now
      Datetimes.filter(_.id === 101L.bind).map(_.datetimetz).update(now1)
      assertEquals(now1, Datetimes.filter(_.id === 101L.bind).map(_.datetimetz).first)

      val now2 = LocalTime.now
      Datetimes.filter(_.id === 101L.bind).map(_.time).update(now2)
      assertEquals(now2, Datetimes.filter(_.id === 101L.bind).map(_.time).first)
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Test
  def testPlainDateFunctions(): Unit = {
    import MyPostgresDriver.plainImplicits._

    implicit val getDateBean = GetResult(r => DatetimeBean(
      r.nextLong(), r.nextLocalDate(), r.nextLocalTime(), r.nextLocalDateTime(), r.nextZonedDateTime(), r.nextPeriod()))

    db withSession { implicit session: Session =>
      Try { Q.updateNA("drop table if exists DatetimeJodaTest cascade").execute }
      Try {
        Q.updateNA("create table DatetimeJodaTest("+
          "id int8 not null primary key, " +
          "date date not null, " +
          "time time not null, " +
          "ts timestamp not null, " +
          "tstz timestamptz not null, " +
          "period interval not null)"
        ).execute
      }
      (Q.u + "SET TIMEZONE TO '+8';").execute

      val dateBean = new DatetimeBean(107L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01.101357"),
        LocalDateTime.parse("2001-01-03T13:21:00.223571"), DateTime.parse("2001-01-03 13:21:00.102203+08", jodaTzDateTimeFormatter),
        Period.parse("P1DT1H1M0.335701S"))

      (Q.u + "insert into DatetimeJodaTest values(" +?dateBean.id + ", " +? dateBean.date + ", " +? dateBean.time
        + ", " +? dateBean.dateTime + ", " +? dateBean.dateTimetz + ", " +? dateBean.interval + ")").execute

      val found = (Q[DatetimeBean] + "select * from DatetimeJodaTest where id = " +? dateBean.id).first

      assertEquals(dateBean, found)
    }
  }
}
