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

  object MyPostgresProfile1 extends ExPostgresProfile with PgArraySupport {
    override val api = new API with ArrayImplicits with MyArrayImplicitsPlus {}

    ///
    trait MyArrayImplicitsPlus {
      implicit val simpleLongBufferTypeMapper = new SimpleArrayJdbcType[Long]("int8").to(_.toBuffer)
      implicit val simpleStrVectorTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toVector)
      implicit val institutionListTypeWrapper =  new SimpleArrayJdbcType[Long]("int8")
        .mapTo[Institution](new Institution(_), _.value).to(_.toList)
      implicit val marketFinancialProductWrapper = new SimpleArrayJdbcType[String]("text")
        .mapTo[MarketFinancialProduct](new MarketFinancialProduct(_), _.value).to(_.toList)
      ///
      implicit val advancedStringListTypeMapper = new AdvancedArrayJdbcType[String]("text",
        fromString(identity)(_).orNull, mkString(identity))
    }
  }

  //////////////////////////////////////////////////////////////////////////
  import MyPostgresProfile1.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class ArrayBean(
    id: Long,
    intArr: List[Int],
    longArr: Buffer[Long],
    shortArr: List[Short],
    strList: List[String],
    strArr: Option[Vector[String]],
    uuidArr: List[UUID],
    institutions: List[Institution],
    mktFinancialProducts: Option[List[MarketFinancialProduct]]
    )

  class ArrayTestTable(tag: Tag) extends Table[ArrayBean](tag, "ArrayTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def intArr = column[List[Int]]("intArray", O.Default(Nil))
    def longArr = column[Buffer[Long]]("longArray")
    def shortArr = column[List[Short]]("shortArray")
    def strList = column[List[String]]("stringList")
    def strArr = column[Option[Vector[String]]]("stringArray")
    def uuidArr = column[List[UUID]]("uuidArray")
    def institutions = column[List[Institution]]("institutions")
    def mktFinancialProducts = column[Option[List[MarketFinancialProduct]]]("mktFinancialProducts")

    def * = (id, intArr, longArr, shortArr, strList, strArr, uuidArr, institutions, mktFinancialProducts) <> (ArrayBean.tupled, ArrayBean.unapply)
  }
  val ArrayTests = TableQuery[ArrayTestTable]

  //------------------------------------------------------------------------------

  val uuid1 = UUID.randomUUID()
  val uuid2 = UUID.randomUUID()
  val uuid3 = UUID.randomUUID()

  val testRec1 = ArrayBean(33L, List(101, 102, 103), Buffer(1L, 3L, 5L, 7L), List(1,7), List("robert}; drop table students--"),
    Some(Vector("str1", "str3")), List(uuid1, uuid2), List(Institution(113)), None)
  val testRec2 = ArrayBean(37L, List(101, 103), Buffer(11L, 31L, 5L), Nil, List(""),
    Some(Vector("str11", "str3")), List(uuid1, uuid2, uuid3), List(Institution(579)), Some(List(MarketFinancialProduct("product1"))))
  val testRec3 = ArrayBean(41L, List(103, 101), Buffer(11L, 5L, 31L), List(35,77), Nil,
    Some(Vector("(s)", "str5", "str3")), List(uuid1, uuid3), Nil, Some(List(MarketFinancialProduct("product3"), MarketFinancialProduct("product x"))))

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
        r.<<[Seq[Short]].to[Vector],
        r.<<[Seq[Float]].toList,
        r.<<[Seq[Double]].toList,
        r.<<[Seq[Boolean]],
        r.<<[Seq[Date]].toList,
        r.<<[Seq[Time]].toList,
        r.<<[Seq[Timestamp]],
        r.<<[Seq[Institution]].toList
      )
    }

    val b = ArrayBean1(101L, "tt".getBytes, List(UUID.randomUUID()), Some(List("tewe", "ttt")), List(111L), List(1, 2), Vector(3, 5), List(1.2f, 43.32f), List(21.35d), List(true, true),
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
