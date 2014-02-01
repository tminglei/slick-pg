package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import org.threeten.bp.{Duration, LocalDateTime, LocalTime, LocalDate, ZonedDateTime}

class PgDate2SupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class DatetimeBean(
    id: Long,
    date: LocalDate,
    time: LocalTime,
    dateTime: LocalDateTime,
    dateTimetz: ZonedDateTime,
    duration: Duration
    )

  class DatetimeTable(tag: Tag) extends Table[DatetimeBean](tag,"Datetime2Test") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def date = column[LocalDate]("date")
    def time = column[LocalTime]("time")
    def datetime = column[LocalDateTime]("datetime")
    def dateTimetz = column[ZonedDateTime]("dateTimetz")
    def duration = column[Duration]("duration")

    def * = (id, date, time, datetime, dateTimetz, duration) <> (DatetimeBean.tupled, DatetimeBean.unapply)
  }
  val Datetimes = TableQuery[DatetimeTable]

  //------------------------------------------------------------------------------

  val testRec1 = new DatetimeBean(101L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01"),
    LocalDateTime.parse("2001-01-03T13:21:00"), ZonedDateTime.parse("2001-01-03 13:21:00+08", tzDateTimeFormatter), Duration.parse("P1DT1H"))
  val testRec2 = new DatetimeBean(102L, LocalDate.parse("2011-03-02"), LocalTime.parse("03:14:07"),
    LocalDateTime.parse("2012-05-08T11:31:06"), ZonedDateTime.parse("2012-05-08 11:31:06-05", tzDateTimeFormatter), Duration.parse("P1587D"))
  val testRec3 = new DatetimeBean(103L, LocalDate.parse("2000-05-19"), LocalTime.parse("11:13:34"),
    LocalDateTime.parse("2019-11-03T13:19:03"), ZonedDateTime.parse("2019-11-03 13:19:03+03", tzDateTimeFormatter), Duration.parse("PT63H16M2S"))

  @Test
  def testDatetimeFunctions(): Unit = {
    db withSession { implicit session: Session =>
      Datetimes forceInsertAll (testRec1, testRec2, testRec3)

      // datetime - '+'/'-'
      val q1 = Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time)
      println(s"[date] '+' sql = ${q1.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-03T12:33:01"), q1.first())

      val q101 = Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date)
      println(s"[date] '+' sql = ${q101.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-03T12:33:01"), q101.first())

      val q2 = Datetimes.filter(_.id === 101L.bind).map(r => r.date +++ r.duration)
      println(s"[date] '+++' sql = ${q2.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-04T01:00:00"), q2.first())

      val q3 = Datetimes.filter(_.id === 101L.bind).map(r => r.time +++ r.duration)
      println(s"[date] '+++' sql = ${q3.selectStatement}")
      assertEquals(LocalTime.parse("13:33:01"), q3.first())

      val q4 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime +++ r.duration)
      println(s"[date] '+++' sql = ${q4.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-04T14:21:00"), q4.first())

      val q5 = Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind)
      println(s"[date] '++' sql = ${q5.selectStatement}")
      assertEquals(LocalDate.parse("2010-11-10"), q5.first())

      val q6 = Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind)
      println(s"[date] '--' sql = ${q6.selectStatement}")
      assertEquals(LocalDate.parse("2010-11-02"), q6.first())

      val q7 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime -- r.time)
      println(s"[date] '--' sql = ${q7.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-03T00:47:59"), q7.first())

      val q8 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime - r.date)
      println(s"[date] '-' sql = ${q8.selectStatement}")
      assertEquals(Duration.parse("-P3589DT10H39M"), q8.first())

      val q801 = Datetimes.filter(_.id === 101L.bind).map(r => r.date.asColumnOf[LocalDateTime] - r.datetime)
      println(s"[date] '-' sql = ${q801.selectStatement}")
      assertEquals(Duration.parse("P3589DT10H39M"), q801.first())

      val q9 = Datetimes.filter(_.id === 101L.bind).map(r => r.date - LocalDate.parse("2009-07-05"))
      println(s"[date] '-' sql = ${q9.selectStatement}")
      assertEquals(485, q9.first())

      val q10 = Datetimes.filter(_.id === 101L.bind).map(r => r.time - LocalTime.parse("02:37:00").bind)
      println(s"[date] '-' sql = ${q10.selectStatement}")
      assertEquals(Duration.parse("PT9H56M1S"), q10.first())

      val q11 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime --- r.duration)
      println(s"[date] '---' sql = ${q11.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-02T12:21:00"), q11.first())

      val q12 = Datetimes.filter(_.id === 101L.bind).map(r => r.time --- r.duration)
      println(s"[date] '---' sql = ${q12.selectStatement}")
      assertEquals(LocalTime.parse("11:33:01"), q12.first())

      val q13 = Datetimes.filter(_.id === 101L.bind).map(r => r.date --- r.duration)
      println(s"[date] '---' sql = ${q13.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-01T23:00:00"), q13.first())

      // datetime - age/part/trunc
      val q14 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.age)
      val q141 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.age(Functions.currentDate.asColumnOf[LocalDateTime]))
      println(s"[date] 'age' sql = ${q14.selectStatement}")
      println(s"[date] 'age' sql1 = ${q141.selectStatement}")
      assertEquals(q141.first(), q14.first())

      val q15 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.part("year"))
      println(s"[date] 'part' sql = ${q15.selectStatement}")
      assertEquals(2001, q15.first(), 0.00001d)

      val q1501 = Datetimes.filter(_.id === 102L.bind).map(r => r.duration.part("year"))
      println(s"[date] 'part' sql = ${q1501.selectStatement}")
      assertEquals(0, q1501.first(), 0.00001d)

      val q16 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.trunc("day"))
      println(s"[date] 'trunc' sql = ${q16.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-03T00:00:00"), q16.first())

      // interval test cases
      val q21 = Datetimes.filter(_.id === 101L.bind).map(r => r.duration + Duration.parse("PT3H").bind)
      println(s"[date] '+' sql = ${q21.selectStatement}")
      assertEquals(Duration.parse("P1DT4H"), q21.first())

      val q22 = Datetimes.filter(_.id === 101L.bind).map(r => -r.duration)
      println(s"[date] 'unary_-' sql = ${q22.selectStatement}")
      assertEquals(Duration.parse("-P1DT1H"), q22.first())

      val q23 = Datetimes.filter(_.id === 101L.bind).map(r => r.duration - Duration.parse("PT2H").bind)
      println(s"[date] '-' sql = ${q23.selectStatement}")
      assertEquals(Duration.parse("P1DT-1H"), q23.first())

      val q24 = Datetimes.filter(_.id === 101L.bind).map(r => r.duration * 3.5)
      println(s"[date] '*' sql = ${q24.selectStatement}")
      assertEquals(Duration.parse("P3DT15H30M"), q24.first())

      val q25 = Datetimes.filter(_.id === 101L.bind).map(r => r.duration / 5.0)
      println(s"[date] '*' sql = ${q25.selectStatement}")
      assertEquals(Duration.parse("PT5H"), q25.first())

      val q26 = Datetimes.filter(_.id === 102L.bind).map(r => r.duration.justifyDays)
      println(s"[date] 'justifyDays' sql = ${q26.selectStatement}")
      assertEquals(Duration.parse("P1587D"), q26.first())

      val q27 = Datetimes.filter(_.id === 103L.bind).map(r => r.duration.justifyHours)
      println(s"[date] 'justifyHours' sql = ${q27.selectStatement}")
      assertEquals(Duration.parse("P2DT15H16M2S"), q27.first())

      val q28 = Datetimes.filter(_.id === 103L.bind).map(r => r.duration.justifyInterval)
      println(s"[date] 'justifyInterval' sql = ${q28.selectStatement}")
      assertEquals(Duration.parse("P2DT15H16M2S"), q28.first())
      
      // timestamp with time zone cases
      val q34 = Datetimes.filter(_.id === 101L.bind).map(r => r.dateTimetz.age)
      val q341 = Datetimes.filter(_.id === 101L.bind).map(r => r.dateTimetz.age(Functions.currentDate.asColumnOf[ZonedDateTime]))
      println(s"[date] 'age' sql = ${q34.selectStatement}")
      println(s"[date] 'age' sql1 = ${q341.selectStatement}")
      assertEquals(q341.first(), q34.first())

      val q35 = Datetimes.filter(_.id === 101L.bind).map(r => r.dateTimetz.part("year"))
      println(s"[date] 'part' sql = ${q35.selectStatement}")
      assertEquals(2001, q35.first(), 0.00001d)

      val q36 = Datetimes.filter(_.id === 101L.bind).map(r => r.dateTimetz.trunc("day"))
      println(s"[date] 'trunc' sql = ${q36.selectStatement}")
      assertEquals(ZonedDateTime.parse("2001-01-03 00:00:00+08", tzDateTimeFormatter), q36.first())
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      Datetimes.ddl create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      Datetimes.ddl drop
    }
  }
}
