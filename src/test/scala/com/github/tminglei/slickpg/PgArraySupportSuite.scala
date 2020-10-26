package com.github.tminglei.slickpg

import java.sql.{Date, Time, Timestamp}
import java.util.UUID

import org.scalatest.FunSuite
import slick.jdbc.GetResult

import scala.collection.mutable.Buffer
import scala.concurrent.Await
import scala.concurrent.duration._

class PgArraySupportSuite extends FunSuite {
  import utils.SimpleArrayUtils._

  //-- additional definitions
  case class Institution(value: Long)
  case class MarketFinancialProduct(value: String)

  trait MyPostgresProfile1 extends ExPostgresProfile with PgArraySupport {
    override val api: API = new API {}

    ///
    trait API extends super.API with ArrayImplicits {
      implicit val simpleOptStrListListMapper = new SimpleArrayJdbcType[String]("text")
        .mapTo[Option[String]](Option(_), _.orNull).to(_.toList)
      implicit val simpleLongBufferTypeMapper = new SimpleArrayJdbcType[Long]("int8").to(_.toBuffer[Long], (v: Buffer[Long]) => v.toSeq)
      implicit val simpleStrVectorTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toVector)
      implicit val institutionListTypeWrapper =  new SimpleArrayJdbcType[Long]("int8")
        .mapTo[Institution](new Institution(_), _.value).to(_.toList)
      implicit val marketFinancialProductWrapper = new SimpleArrayJdbcType[String]("text")
        .mapTo[MarketFinancialProduct](new MarketFinancialProduct(_), _.value).to(_.toList)
      ///
      implicit val bigDecimalTypeWrapper = new SimpleArrayJdbcType[java.math.BigDecimal]("numeric")
        .mapTo[scala.math.BigDecimal](javaBigDecimal => scala.math.BigDecimal(javaBigDecimal),
          scalaBigDecimal => scalaBigDecimal.bigDecimal).to(_.toList)
      implicit val advancedStringListTypeMapper = new AdvancedArrayJdbcType[String]("text",
        fromString(identity)(_).orNull, mkString(identity))
      ///
      implicit val longlongWitness = ElemWitness.AnyWitness.asInstanceOf[ElemWitness[List[Long]]]
      implicit val simpleLongLongListTypeMapper = new SimpleArrayJdbcType[List[Long]]("int8[]")
        .to(_.asInstanceOf[Seq[Array[Any]]].toList.map(_.toList.asInstanceOf[List[Long]]))
    }
  }
  object MyPostgresProfile1 extends MyPostgresProfile1

  //////////////////////////////////////////////////////////////////////////
  import MyPostgresProfile1.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class ArrayBean(
    id: Long,
    str: String,
    intArr: List[Int],
    longArr: Buffer[Long],
    longlongArr: List[List[Long]],
    shortArr: List[Short],
    strList: List[String],
    optStrList: List[Option[String]],
    strArr: Option[Vector[String]],
    uuidArr: List[UUID],
    bigDecimalArr: List[BigDecimal],
    institutions: List[Institution],
    mktFinancialProducts: Option[List[MarketFinancialProduct]]
  )

  class ArrayTestTable(tag: Tag) extends Table[ArrayBean](tag, "ArrayTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def str = column[String]("str")
    def intArr = column[List[Int]]("intArray", O.Default(Nil))
    def longArr = column[Buffer[Long]]("longArray")
    def longlongArr = column[List[List[Long]]]("longlongArray")
    def shortArr = column[List[Short]]("shortArray")
    def strList = column[List[String]]("stringList")
    def optStrList = column[List[Option[String]]]("optStrList")
    def strArr = column[Option[Vector[String]]]("stringArray")
    def uuidArr = column[List[UUID]]("uuidArray")
    def bigDecimalArr = column[List[BigDecimal]]("bigDecimalArr")
    def institutions = column[List[Institution]]("institutions")
    def mktFinancialProducts = column[Option[List[MarketFinancialProduct]]]("mktFinancialProducts")

    def * = (id, str, intArr, longArr, longlongArr, shortArr, strList, optStrList, strArr, uuidArr,
      bigDecimalArr, institutions, mktFinancialProducts) <> (ArrayBean.tupled, ArrayBean.unapply)
  }
  val ArrayTests = TableQuery[ArrayTestTable]

  //------------------------------------------------------------------------------

  val uuid1 = UUID.randomUUID()
  val uuid2 = UUID.randomUUID()
  val uuid3 = UUID.randomUUID()

  val testRec1 = ArrayBean(33L, "tt", List(101, 102, 103), Buffer(1L, 3L, 5L, 7L), List(List(11L, 12L, 13L)), List(1,7),
    List("robert}; drop table students--", null, "NULL"), List(Some("[2.3,)"), Some("[0.3.0,)"), None, Some("7.1.0"), None),
    Some(Vector("str1", "str3", "", " ")), List(uuid1, uuid2), List(BigDecimal.decimal(0.5)), List(Institution(113)), None)
  val testRec2 = ArrayBean(37L, "test'", List(101, 103), Buffer(11L, 31L, 5L), List(List(21L, 22L, 23L)), Nil, List(""), Nil,
    Some(Vector("str11", "str3")), List(uuid1, uuid2, uuid3), Nil, List(Institution(579)), Some(List(MarketFinancialProduct("product1"))))
  val testRec3 = ArrayBean(41L, "haha", List(103, 101), Buffer(11L, 5L, 31L), List(List(31L, 32L, 33L)), List(35,77), Nil, Nil,
    Some(Vector("(s)", "str5", "str3")), List(uuid1, uuid3), Nil, Nil, Some(List(MarketFinancialProduct("product3"), MarketFinancialProduct("product x"))))

  test("Array Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        (ArrayTests.schema) create,
        ArrayTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          ArrayTests.sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // all
          ArrayTests.filter(101.bind === _.intArr.any).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // all
          ArrayTests.filter(5L.bind <= _.longArr.all).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2, testRec3) === r)
          ),
          // all
          ArrayTests.filter(_.str === (List("test'") : Rep[List[String]]).any).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          // @>
          ArrayTests.filter(_.strArr @> Vector("str3")).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          ArrayTests.filter(_.strArr @> Vector("str3").bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // <@
          ArrayTests.filter(Vector("str3").bind <@: _.strArr).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // &&
          ArrayTests.filter(_.longArr @& Buffer(5L, 17L).bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // length
          ArrayTests.filter(_.longArr.length() > 3.bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1) === r)
          ),
          ArrayTests.filter(_.shortArr.length() === 0.bind).map(_.id).to[List].result.map(
            r => assert(List(37L) === r)
          ),
          // unnest
          ArrayTests.filter(5L.bind <= _.longArr.all).map(_.strArr.unnest).to[List].result.map(
            r => assert((testRec2.strArr.get ++ testRec3.strArr.get).toList === r.map(_.orNull))
          ),
          // concatenate
          ArrayTests.filter(_.id === 33L.bind).map(_.intArr ++ List(105, 107).bind).result.head.map(
            r => assert(List(101, 102, 103, 105, 107) === r)
          ),
          ArrayTests.filter(_.id === 33L.bind).map(List(105, 107).bind ++ _.intArr).result.head.map(
            r => assert(List(105, 107, 101, 102, 103) === r)
          ),
          ArrayTests.filter(_.id === 33L.bind).map(_.intArr + 105.bind).result.head.map(
            r => assert(List(101, 102, 103, 105) === r)
          ),
          ArrayTests.filter(_.id === 33L.bind).map(105.bind +: _.intArr).result.head.map(
            r => assert(List(105, 101, 102, 103) === r)
          )
        )
      ).andFinally(
        (ArrayTests.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  //------------------------------------------------------------------------

  case class ArrayBean1(
    id: Long,
    bytea: Array[Byte],
    uuidArr: List[UUID],
    strArr: Option[List[String]],
    longArr: Seq[Long],
    intArr: List[Int],
    shortArr: Vector[Short],
    floatArr: List[Float],
    doubleArr: List[Double],
    boolArr: Seq[Boolean],
    dateArr: List[Date],
    timeArr: List[Time],
    tsArr: Seq[Timestamp],
    institutionArr: List[Institution]
  )

  test("Array Plain SQL support") {
    import MyPostgresProfile.plainAPI._
    import utils.PlainSQLUtils._

    {
      addNextArrayConverter((r) => r.nextArrayOption[Long]().map(_.map(Institution(_))))
    }

    implicit val getInstitutionArray = mkGetResult(_.nextArray[Institution]())
    implicit val getInstitutionArrayOption = mkGetResult(_.nextArrayOption[Institution]())
    implicit val setInstitutionArray = mkArraySetParameter[Institution]("int8", v => String.valueOf(v.value))
    implicit val setInstitutionArrayOption = mkArrayOptionSetParameter[Institution]("int8", v => String.valueOf(v.value))

    implicit val getArrarBean1Result = GetResult { r =>
      ArrayBean1(r.nextLong(),
        r.<<[Array[Byte]],
        r.<<[Seq[UUID]].toList,
        r.<<?[Seq[String]].map(_.toList),
        r.<<[Seq[Long]],
        r.<<[Seq[Int]].toList,
        r.<<[Seq[Short]].toVector,
        r.<<[Seq[Float]].toList,
        r.<<[Seq[Double]].toList,
        r.<<[Seq[Boolean]],
        r.<<[Seq[Date]].toList,
        r.<<[Seq[Time]].toList,
        r.<<[Seq[Timestamp]],
        r.<<[Seq[Institution]].toList
      )
    }

    val b = ArrayBean1(101L, "tt".getBytes, List(UUID.randomUUID()), Some(List("tewe", "ttt", "apostrophe'")), List(111L), List(1, 2), Vector(3, 5), List(1.2f, 43.32f), List(21.35d), List(true, true),
      List(new Date(System.currentTimeMillis())), List(new Time(System.currentTimeMillis())), List(new Timestamp(System.currentTimeMillis())), List(Institution(579)))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table ArrayTest1(
                 id int8 not null primary key,
                 byte_arr bytea not null,
                 uuid_arr uuid[] not null,
                 str_arr text[] not null,
                 long_arr int8[] not null,
                 int_arr int4[] not null,
                 short_arr int2[] not null,
                 float_arr float4[] not null,
                 double_arr float8[] not null,
                 bool_arr bool[] not null,
                 date_arr date[] not null,
                 time_arr time[] not null,
                 ts_arr timestamp[] not null,
                 ins_arr int8[] not null)
            """,
        ///
        sqlu"insert into ArrayTest1 values(${b.id}, ${b.bytea}, ${b.uuidArr}, ${b.strArr}, ${b.longArr}, ${b.intArr}, ${b.shortArr}, ${b.floatArr}, ${b.doubleArr}, ${b.boolArr}, ${b.dateArr}, ${b.timeArr}, ${b.tsArr}, ${b.institutionArr})",
        sql"select * from ArrayTest1 where id = ${b.id}".as[ArrayBean1].head.map(
          f => {
            b.bytea.zip(f.bytea).map(r => assert(r._1 === r._2))
            b.uuidArr.zip(f.uuidArr).map(r => assert(r._1 === r._2))
            b.strArr.getOrElse(Nil).zip(f.strArr.getOrElse(Nil)).map(r => assert(r._1 === r._2))
            b.longArr.zip(f.longArr).map(r => assert(r._1 === r._2))
            b.intArr.zip(f.intArr).map(r => assert(r._1 === r._2))
            b.shortArr.zip(f.shortArr).map(r => assert(r._1 === r._2))
            b.floatArr.zip(f.floatArr).map(r => assert(Math.abs(r._1 - r._2) < 0.01f))
            b.doubleArr.zip(f.doubleArr).map(r => assert(Math.abs(r._1 - r._2) < 0.01d))
            b.boolArr.zip(f.boolArr).map(r => assert(r._1 === r._2))
            b.dateArr.zip(f.dateArr).map(r => assert(r._1.toString === r._2.toString))
            b.timeArr.zip(f.timeArr).map(r => assert(r._1.toString === r._2.toString))
            b.tsArr.zip(f.tsArr).map(r => assert(r._1.toString === r._2.toString))
            b.institutionArr.zip(f.institutionArr).map(r => assert(r._1 === r._2))
          }
        ),
        ///
        sqlu"drop table if exists ArrayTest1 cascade"
      ).transactionally
    ), Duration.Inf)
  }
}
