package com.github.tminglei.slickpg

import scala.collection.compat._
import scala.collection.compat.immutable.LazyList
import scala.concurrent.Await
import scala.concurrent.duration._
import slick.jdbc.{JdbcType, PostgresProfile}
import org.scalatest.funsuite.AnyFunSuite


class PgEnumSupportSuite extends AnyFunSuite with PostgresContainer {
  object WeekDays extends Enumeration {
    type WeekDay = Value
    val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
  }
  object Rainbows extends Enumeration {
    type Rainbow = Value
    val red, orange, yellow, green, blue, purple = Value
  }

  ///---
  trait Enum[A] {
    trait Value { self: A =>
      _values :+= this
    }
    private var _values = List.empty[A]
    def values = _values map (v => (v.toString, v)) toMap
  }

  sealed trait Currency extends Currency.Value
  object Currency extends Enum[Currency]
  case object EUR extends Currency
  case object GBP extends Currency
  case object USD extends Currency

  sealed abstract class Gender(val repr: String)
  case object Female extends Gender("f")
  case object Male extends Gender("m")
  object Gender {
    def fromString(s: String): Gender = s match {
      case "f" => Female
      case "m" => Male
    }
    def values = Seq(Female, Male)
  }

  /////////////////////////////////////////////////////////////////////

  import Rainbows._
  import WeekDays._

  trait MyPostgresProfile1 extends PostgresProfile with PgEnumSupport {

    override val api: API = new API {}

    ///
    trait API extends JdbcAPI {
      implicit val weekDayTypeMapper: JdbcType[WeekDays.Value] = createEnumJdbcType[WeekDays.type]("WeekDay", WeekDays)
      implicit val weekDayListTypeMapper: JdbcType[List[WeekDays.Value]] = createEnumListJdbcType[WeekDays.type]("weekDay", WeekDays)
      implicit val rainbowTypeMapper: JdbcType[Rainbows.Value] = createEnumJdbcType[Rainbows.type]("Rainbow", Rainbows, true)
      implicit val rainbowListTypeMapper: JdbcType[List[Rainbows.Value]] = createEnumListJdbcType[Rainbows.type]("Rainbow", Rainbows, true)

      implicit def weekDayColumnExtensionMethodsBuilder(rep: Rep[WeekDays.Value]): EnumColumnExtensionMethods[WeekDays.Value, WeekDays.Value] = createEnumColumnExtensionMethodsBuilder[WeekDays.type](WeekDays).apply(rep)
      implicit def weekDayOptionColumnExtensionMethodsBuilder(rep: Rep[Option[WeekDays.Value]]): EnumColumnExtensionMethods[WeekDays.Value, Option[WeekDays.Value]] = createEnumOptionColumnExtensionMethodsBuilder[WeekDays.type](WeekDays).apply(rep)
      implicit def rainbowColumnExtensionMethodsBuilder(rep: Rep[Rainbows.Value]): EnumColumnExtensionMethods[Rainbows.Value, Rainbows.Value] = createEnumColumnExtensionMethodsBuilder[Rainbows.type](Rainbows).apply(rep)
      implicit def rainbowOptionColumnExtensionMethodsBuilder(rep: Rep[Option[Rainbows.Value]]): EnumColumnExtensionMethods[Rainbows.Value, Option[Rainbows.Value]] = createEnumOptionColumnExtensionMethodsBuilder[Rainbows.type](Rainbows).apply(rep)

      /// custom types of java enums and algebraic data type (ADT)
      implicit val currencyTypeMapper: JdbcType[Currency] = createEnumJdbcType[Currency]("Currency", _.toString, Currency.values.get(_).get, quoteName = false)
      implicit val currencyTypeListMapper: JdbcType[List[Currency]] = createEnumListJdbcType[Currency]("Currency", _.toString, Currency.values.get(_).get, quoteName = false)
      implicit val languagesTypeMapper: JdbcType[Languages] = createEnumJdbcType[Languages]("Languages", _.name(), Languages.valueOf, quoteName = true)
      implicit val languagesTypeListMapper: JdbcType[List[Languages]] = createEnumListJdbcType[Languages]("Languages", _.name(), Languages.valueOf, quoteName = true)
      implicit val genderTypeMapper: JdbcType[Gender] = createEnumJdbcType[Gender]("Gender", _.repr, Gender.fromString, quoteName = false)
      implicit val genderTypeListMapper: JdbcType[List[Gender]] = createEnumListJdbcType[Gender]("Gender", _.repr, Gender.fromString, quoteName = false)

      implicit def currencyColumnExtensionMethodsBuilder(rep: Rep[Currency]): EnumColumnExtensionMethods[Currency, Currency] = createEnumColumnExtensionMethodsBuilder[Currency].apply(rep)
      implicit def currencyOptionColumnExtensionMethodsBuilder(rep: Rep[Option[Currency]]): EnumColumnExtensionMethods[Currency, Option[Currency]] = createEnumOptionColumnExtensionMethodsBuilder[Currency].apply(rep)
      implicit def languagesColumnExtensionMethodsBuilder(rep: Rep[Languages]): EnumColumnExtensionMethods[Languages, Languages] = createEnumColumnExtensionMethodsBuilder[Languages].apply(rep)
      implicit def languagesOptionColumnExtensionMethodsBuilder(rep: Rep[Option[Languages]]): EnumColumnExtensionMethods[Languages, Option[Languages]] = createEnumOptionColumnExtensionMethodsBuilder[Languages].apply(rep)
      implicit def genderColumnExtensionMethodsBuilder(rep: Rep[Gender]): EnumColumnExtensionMethods[Gender, Gender] = createEnumColumnExtensionMethodsBuilder[Gender].apply(rep)
      implicit def genderOptionColumnExtensionMethodsBuilder(rep: Rep[Option[Gender]]): EnumColumnExtensionMethods[Gender, Option[Gender]] = createEnumOptionColumnExtensionMethodsBuilder[Gender].apply(rep)
    }
  }
  object MyPostgresProfile1 extends MyPostgresProfile1

  ////////////////////////////////////////////////////////////////////
  import MyPostgresProfile1.api._

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  case class TestEnumBean(
    id: Long,
    weekday: WeekDay,
    rainbow: Option[Rainbow],
    weekdays: List[WeekDay],
    rainbows: List[Rainbow])

  class TestEnumTable(tag: Tag) extends Table[TestEnumBean](tag, "test_enum_table") {
    def id = column[Long]("id")
    def weekday = column[WeekDay]("weekday", O.Default(Mon))
    def rainbow = column[Option[Rainbow]]("rainbow")
    def weekdays = column[List[WeekDay]]("weekdays")
    def rainbows = column[List[Rainbow]]("rainbows")

    def * = (id, weekday, rainbow, weekdays, rainbows) <> ((TestEnumBean.apply _).tupled, TestEnumBean.unapply)
  }
  val TestEnums = TableQuery(new TestEnumTable(_))

  ///---
  case class TestEnumBean1(
    id: Long,
    currency: Currency,
    language: Option[Languages],
    gender: Gender,
    currencies: List[Currency],
    languages: List[Languages])

  class TestEnumTable1(tag: Tag) extends Table[TestEnumBean1](tag, "test_enum_table_1") {
    def id = column[Long]("id")
    def currency = column[Currency]("currency")
    def language = column[Option[Languages]]("language")
    def gender = column[Gender]("gender")
    def currencies = column[List[Currency]]("currencies")
    def languages = column[List[Languages]]("languages")

    def * = (id, currency, language, gender, currencies, languages) <> ((TestEnumBean1.apply _).tupled, TestEnumBean1.unapply)
  }
  val TestEnums1 = TableQuery(new TestEnumTable1(_))

  //------------------------------------------------------------------

  val testRec1 = TestEnumBean(101L, Mon, Some(red), Nil, List(red, yellow))
  val testRec2 = TestEnumBean(102L, Wed, Some(blue), List(Sat, Sun), List(green))
  val testRec3 = TestEnumBean(103L, Fri, None, List(Thu), Nil)

  val testRec11 = TestEnumBean1(101L, EUR, Some(Languages.SCALA), Male, Nil, List(Languages.SCALA, Languages.CLOJURE))
  val testRec12 = TestEnumBean1(102L, GBP, None, Female, List(EUR, GBP, USD), List(Languages.JAVA))
  val testRec13 = TestEnumBean1(103L, USD, Some(Languages.CLOJURE), Male, List(GBP), Nil)

  test("Enum Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        PgEnumSupportUtils.buildCreateSql("WeekDay", WeekDays),
        PgEnumSupportUtils.buildCreateSql("Rainbow", Rainbows, true),
        (TestEnums.schema) create,
        ///
        TestEnums forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          TestEnums.sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // first
          TestEnums.filter(_.id === 101L.bind).map(t => t.weekday.first).result.head.map(
            r => assert(Mon === r)
          ),
          // last
          TestEnums.filter(_.id === 101L.bind).map(t => t.rainbow.last).result.head.map(
            r => assert(Some(purple) === r)
          ),
          // all
          TestEnums.filter(_.id === 101L.bind).map(t => t.weekday.all).result.head.map(
            r => assert(WeekDays.values.toList === r)
          ),
          // range
          TestEnums.filter(_.id === 102L.bind).map(t => t.weekday range null.asInstanceOf[WeekDay]).result.head.map(
            r => assert(List(Wed, Thu, Fri, Sat, Sun) === r)
          ),
          TestEnums.filter(_.id === 102L.bind).map(t => null.asInstanceOf[WeekDay].bind range t.weekday).result.head.map(
            r => assert(List(Mon, Tue, Wed) === r)
          )
        )
      ).andFinally(
        DBIO.seq(
          (TestEnums.schema) drop,
          PgEnumSupportUtils.buildDropSql("Rainbow", true),
          PgEnumSupportUtils.buildDropSql("weekday")
        )
      ) .transactionally
    ), Duration.Inf)
  }

  test("Custom enum Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        PgEnumSupportUtils.buildCreateSql("Currency", Currency.values.to(LazyList).map(_._1), quoteName = false),
        PgEnumSupportUtils.buildCreateSql("Languages", Languages.values.to(LazyList).map(_.name()), quoteName = true),
        PgEnumSupportUtils.buildCreateSql("Gender", Gender.values.map(_.repr), quoteName = false),
        (TestEnums1.schema) create,
        ///
        TestEnums1 forceInsertAll List(testRec11, testRec12, testRec13)
      ).andThen(
        DBIO.seq(
          TestEnums1.sortBy(_.id).to[List].result.map(
            r => assert(List(testRec11, testRec12, testRec13) === r)
          ),
          // first
          TestEnums1.filter(_.id === 101L.bind).map(t => t.currency.first).result.head.map(
            r => assert(EUR === r)
          ),
          // last
          TestEnums1.filter(_.id === 101L.bind).map(t => t.language.last).result.head.map(
            r => assert(Some(Languages.CLOJURE) === r)
          ),
          // all
          TestEnums1.filter(_.id === 101L.bind).map(t => t.currency.all).result.head.map(
            r => assert(Currency.values.toList.map(_._2) === r)
          ),
          // range
          TestEnums1.filter(_.id === 102L.bind).map(t => t.currency range null.asInstanceOf[Currency]).result.head.map(
            r => assert(List(GBP, USD) === r)
          ),
          TestEnums1.filter(_.id === 102L.bind).map(t => null.asInstanceOf[Currency].bind range t.currency).result.head.map(
            r => assert(List(EUR, GBP) === r)
          ),
          TestEnums1.filter(_.gender === (Female: Gender)).result.map(
            r => assert(List(testRec12) === r)
          )
        )
      ).andFinally(
        DBIO.seq(
          (TestEnums1.schema) drop,
          PgEnumSupportUtils.buildDropSql("Currency"),
          PgEnumSupportUtils.buildDropSql("Languages", true),
          PgEnumSupportUtils.buildDropSql("Gender")
        )
      ) .transactionally
    ), Duration.Inf)
  }
}
