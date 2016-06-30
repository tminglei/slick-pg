package com.github.tminglei.slickpg

import org.scalatest.FunSuite
import slick.driver.PostgresDriver
import sun.security.provider.Sun

import scala.concurrent.Await
import scala.concurrent.duration._

class PgCustomEnumSupportSuite extends FunSuite {

  trait Enum[A] {
    trait Value { self: A =>
      _values :+= this
    }
    private var _values = List.empty[A]
    def values = _values map (v => (v.toString, v)) toMap
  }
  object Currency extends Enum[Currency]
  sealed trait Currency extends Currency.Value
  case object EUR extends Currency
  case object GBP extends Currency
  case object USD extends Currency

  object MyPostgresDriver1 extends PostgresDriver with PgCustomEnumSupport {
    override val api = new API with MyEnumImplicits {}

    trait MyEnumImplicits {
      implicit val currencyTypeMapper = createEnumJdbcType[Currency]("Currency", _.toString, Currency.values.get(_).get)
      implicit val currencyTypeListMapper = createEnumListJdbcType[Currency]("Currency", _.toString, Currency.values.get(_).get)
      implicit val languagesTypeMapper = createEnumJdbcType[Languages]("Languages", _.name(), Languages.valueOf, true)
      implicit val languagesTypeListMapper = createEnumListJdbcType[Languages]("Languages", _.name(), Languages.valueOf, true)

      implicit val currencyColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder[Currency]
      implicit val currencyOptionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder[Currency]
      implicit val languagesColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder[Languages]
      implicit val languagesOptionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder[Languages]
    }
  }

  ////////////////////////////////////////////////////////////////////
  import MyPostgresDriver1.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class TestEnumBean(id: Long,
                          currency: Currency,
                          language: Option[Languages],
                          currencies: List[Currency],
                          languages: List[Languages])

  class TestEnumTable(tag: Tag) extends Table[TestEnumBean](tag, "test_enum_table") {
    def id = column[Long]("id")
    def currency = column[Currency]("currency")
    def language = column[Option[Languages]]("language")
    def currencies = column[List[Currency]]("currencies")
    def languages = column[List[Languages]]("languages")

    def * = (id, currency, language, currencies, languages) <> (TestEnumBean.tupled, TestEnumBean.unapply)
  }
  val TestEnums = TableQuery(new TestEnumTable(_))

  //------------------------------------------------------------------

  val testRec1 = TestEnumBean(101L, EUR, Some(Languages.SCALA), Nil, List(Languages.SCALA, Languages.CLOJURE))
  val testRec2 = TestEnumBean(102L, GBP, None, List(EUR, GBP, USD), List(Languages.JAVA))
  val testRec3 = TestEnumBean(103L, USD, Some(Languages.CLOJURE), List(GBP), Nil)

  test("Enum Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        PgEnumSupportUtils.buildCreateCustomSql("Currency", Currency.values.toStream.map(_._1)),
        PgEnumSupportUtils.buildCreateCustomSql("Languages", Languages.values.toStream.map(_.name()), true),
        (TestEnums.schema) create,
        ///
        TestEnums forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          TestEnums.sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // first
          TestEnums.filter(_.id === 101L.bind).map(t => t.currency.first).result.head.map(
            r => assert(EUR === r)
          ),
          // last
          TestEnums.filter(_.id === 101L.bind).map(t => t.language.last).result.head.map(
            r => assert(Some(Languages.CLOJURE) === r)
          ),
          // all
          TestEnums.filter(_.id === 101L.bind).map(t => t.currency.all).result.head.map(
            r => assert(Currency.values.toList.map(_._2) === r)
          ),
          // range
          TestEnums.filter(_.id === 102L.bind).map(t => t.currency range null.asInstanceOf[Currency]).result.head.map(
            r => assert(List(GBP, USD) === r)
          ),
          TestEnums.filter(_.id === 102L.bind).map(t => null.asInstanceOf[Currency].bind range t.currency).result.head.map(
            r => assert(List(EUR, GBP) === r)
          )
        )
      ).andFinally(
        DBIO.seq(
          (TestEnums.schema) drop,
          PgEnumSupportUtils.buildDropSql("Currency"),
          PgEnumSupportUtils.buildDropSql("Languages", true)
        )
      ) .transactionally
    ), Duration.Inf)
  }
}
