package com.github.tminglei.slickpg

import java.time.LocalDateTime
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.immutable.{Range => ScalaRange}
import scala.jdk.CollectionConverters._
import slick.jdbc.{GetResult, PositionedResult, PostgresProfile, SetParameter}
import com.github.tminglei.slickpg.composite.Struct
import org.postgresql.util.HStoreConverter
import org.scalatest.funsuite.AnyFunSuite

import com.github.tminglei.slickpg.utils.{RegisteredTypeConverter,TypeConverters}


object PgCompositeSupportSuite {

  import TypeConverters.*
  def ts(str: String) = LocalDateTime.parse(str.replace(' ', 'T'))

  case class Composite1(
    id: Long,
    txt: String,
    date: LocalDateTime,
    tsRange: Option[Range[LocalDateTime]]
  ) extends Struct

  case class Composite2(
    id: Long,
    comp1: Composite1,
    confirm: Boolean,
    map: Map[String, String]
  ) extends Struct

  case class Composite3(
    name: Option[String] = None,
    code: Option[Int] = None,
    num: Option[Int] = None,
    bool: Option[Boolean] = None
  ) extends Struct

  case class Composite4(
    id: Int,
    name: String,
    variable: List[String],
    optVars: Option[List[String]],
    script1: String,
    script2: String
  ) extends Struct

  case class C1 (
    name: String
  ) extends Struct

  case class C2 (
    c1: List[C1]
  ) extends Struct

  //-------------------------------------------------------------
  trait MyPostgresProfile1 extends PostgresProfile with PgCompositeSupport with PgArraySupport with utils.PgCommonJdbcTypes {
    def mapToString(m: Map[String, String]): String = HStoreConverter.toString((m).asJava)
    def stringToMap(s: String): Map[String, String] = (HStoreConverter.fromString(s)
      .asInstanceOf[java.util.Map[String, String]]).asScala.toMap

    ///
    trait API extends JdbcAPI with ArrayImplicits {
      implicit val StringToRange: RegisteredTypeConverter[String, Range[LocalDateTime]] =
        RegisteredTypeConverter(PgRangeSupportUtils.mkRangeFn(ts))
      implicit val RangeToString: RegisteredTypeConverter[Range[LocalDateTime], String] =
        RegisteredTypeConverter(PgRangeSupportUtils.toStringFn[LocalDateTime](_.toString))
      implicit val MapToString: RegisteredTypeConverter[Map[String, String], String] = RegisteredTypeConverter(mapToString)
      implicit val StringToMap: RegisteredTypeConverter[String, Map[String, String]] = RegisteredTypeConverter(stringToMap)

      implicit val composite1TypeMapper: GenericJdbcType[Composite1] = createCompositeJdbcType[Composite1]("composite1")
      implicit val composite2TypeMapper: GenericJdbcType[Composite2] = createCompositeJdbcType[Composite2]("composite2")
      implicit val composite3TypeMapper: GenericJdbcType[Composite3] = createCompositeJdbcType[Composite3]("composite3")
      implicit val composite4TypeMapper: GenericJdbcType[Composite4] = createCompositeJdbcType[Composite4]("composite4")
      implicit val c1TypeMapper: GenericJdbcType[C1] = createCompositeJdbcType[C1]("c1")
      implicit val c2TypeMapper: GenericJdbcType[C2] = createCompositeJdbcType[C2]("c2")

      implicit val composite1ArrayTypeMapper: DriverJdbcType[List[Composite1]] = createCompositeArrayJdbcType[Composite1]("composite1").to(_.toList)
      implicit val composite2ArrayTypeMapper: DriverJdbcType[List[Composite2]] = createCompositeArrayJdbcType[Composite2]("composite2").to(_.toList)
      implicit val composite3ArrayTypeMapper: DriverJdbcType[List[Composite3]] = createCompositeArrayJdbcType[Composite3]("composite3").to(_.toList)
    }
    override val api: API = new API {}

    object plainImplicits extends API with SimpleArrayPlainImplicits {
      import utils.PlainSQLUtils._
      // to support 'nextArray[T]/nextArrayOption[T]' in PgArraySupport
      {
        addNextArrayConverter((r) => nextCompositeArray[Composite1](r))
        addNextArrayConverter((r) => nextCompositeArray[Composite2](r))
        addNextArrayConverter((r) => nextCompositeArray[Composite3](r))
      }

      implicit class MyCompositePositionedResult(r: PositionedResult) {
        def nextComposite1() = nextComposite[Composite1](r)
        def nextComposite2() = nextComposite[Composite2](r)
        def nextComposite3() = nextComposite[Composite3](r)
      }

      implicit val composite1SetParameter: SetParameter[Composite1] = createCompositeSetParameter[Composite1]("composite1")
      implicit val composite1OptSetParameter: SetParameter[Option[Composite1]] = createCompositeOptionSetParameter[Composite1]("composite1")
      implicit val composite1ArraySetParameter: SetParameter[Seq[Composite1]] = createCompositeArraySetParameter[Composite1]("composite1")
      implicit val composite1ArrayOptSetParameter: SetParameter[Option[Seq[Composite1]]] = createCompositeOptionArraySetParameter[Composite1]("composite1")

      implicit val composite2SetParameter: SetParameter[Composite2] = createCompositeSetParameter[Composite2]("composite2")
      implicit val composite2OptSetParameter: SetParameter[Option[Composite2]] = createCompositeOptionSetParameter[Composite2]("composite2")
      implicit val composite2ArraySetParameter: SetParameter[Seq[Composite2]] = createCompositeArraySetParameter[Composite2]("composite2")
      implicit val composite2ArrayOptSetParameter: SetParameter[Option[Seq[Composite2]]] = createCompositeOptionArraySetParameter[Composite2]("composite2")

      implicit val composite3SetParameter: SetParameter[Composite3] = createCompositeSetParameter[Composite3]("composite3")
      implicit val composite3OptSetParameter: SetParameter[Option[Composite3]] = createCompositeOptionSetParameter[Composite3]("composite3")
      implicit val composite3ArraySetParameter: SetParameter[Seq[Composite3]] = createCompositeArraySetParameter[Composite3]("composite3")
      implicit val composite3ArrayOptSetParameter: SetParameter[Option[Seq[Composite3]]] = createCompositeOptionArraySetParameter[Composite3]("composite3")
    }
  }
  object MyPostgresProfile1 extends MyPostgresProfile1
}

///
class PgCompositeSupportSuite extends AnyFunSuite with PostgresContainer {
  import PgCompositeSupportSuite._
  import MyPostgresProfile1.api._

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  case class TestBean(
    id: Long,
    comps: List[Composite2]
  )

  case class TestBean1(
    id: Long,
    comps: List[Composite3]
  )

  case class TestBean2(
    id: Long,
    comps: Composite1,
    c2: C2
  )

  case class TestBean3(
    id: Long,
    comps: Option[Composite4]
  )

  class TestTable(tag: Tag) extends Table[TestBean](tag, "CompositeTest") {
    def id = column[Long]("id")
    def comps = column[List[Composite2]]("comps", O.Default(Nil))

    def * = (id,comps) <> ((TestBean.apply _).tupled, TestBean.unapply)
  }
  val CompositeTests = TableQuery(new TestTable(_))

  class TestTable1(tag: Tag) extends Table[TestBean1](tag, "CompositeTest1") {
    def id = column[Long]("id")
    def comps = column[List[Composite3]]("comps")

    def * = (id,comps) <> ((TestBean1.apply _).tupled, TestBean1.unapply)
  }
  val CompositeTests1 = TableQuery(new TestTable1(_))

  class TestTable2(tag: Tag) extends Table[TestBean2](tag, "CompositeTest2") {
    def id = column[Long]("id")
    def comps = column[Composite1]("comp")
    def c2 = column[C2]("c2")

    def * = (id,comps,c2) <> ((TestBean2.apply _).tupled, TestBean2.unapply)
  }
  val CompositeTests2 = TableQuery(new TestTable2(_))

  class TestTable3(tag: Tag) extends Table[TestBean3](tag, "CompositeTest3") {
    def id = column[Long]("id")
    def comps = column[Option[Composite4]]("comp")

    def * = (id, comps) <> ((TestBean3.apply _).tupled, TestBean3.unapply)
  }
  val CompositeTests3 = TableQuery(new TestTable3(_))

  //-------------------------------------------------------------------

  val rec1 = TestBean(333, List(Composite2(201, Composite1(101, "(test1'", ts("2001-01-03T13:21:00"),
    Some(Range(ts("2010-01-01T14:30:00"), ts("2010-01-03T15:30:00")))), true, Map("t" -> "haha", "t2" -> "133"))))
  val rec2 = TestBean(335, List(Composite2(202, Composite1(102, "test2\\", ts("2012-05-08T11:31:06"),
    Some(Range(ts("2011-01-01T14:30:00"), ts("2011-11-01T15:30:00")))), false, Map("t't" -> "1,363"))))
  val rec3 = TestBean(337, List(Composite2(203, Composite1(103, "getRate(\"x\") + 5;", ts("2015-03-08T17:17:03"),
    None), false, Map("t,t" -> "getRate(\"x\") + 5;"))))
  val rec4 = TestBean(339, List(Composite2(204, Composite1(104, "x=1&y=2&[INSERT_DEVICE_ID_HERE]&z=3", ts("2001-01-03T13:21:00"),
    Some(Range(ts("2010-01-01T14:30:00"), ts("2010-01-03T15:30:00")))), true, Map("t" -> "haha", "t2" -> "133"))))

  val rec11 = TestBean1(111, List(Composite3(Some("(test1'"))))
  val rec12 = TestBean1(112, List(Composite3(code = Some(102))))
  val rec13 = TestBean1(113, List(Composite3()))
  val rec14 = TestBean1(114, List(Composite3(Some("Word1 (Word2)"))))
  val rec15 = TestBean1(115, List(Composite3(Some(""))))

  val rec21 = TestBean2(211, Composite1(201, "test3", ts("2015-01-03T13:21:00"),
    Some(Range(ts("2015-01-01T14:30:00"), ts("2016-01-03T15:30:00")))), C2(List(C1("1s"))))
  val rec22 = TestBean2(212, Composite1(202, "", ts("2015-01-03T13:21:00"),
    Some(Range(ts("2015-01-01T14:30:00"), ts("2016-01-03T15:30:00")))), C2(List(C1("test1"))))

  val rec31 = TestBean3(1, None)
  val rec32 = TestBean3(2, Some(Composite4(1, "x1", Nil, Some(List.empty), "get(\"x1\").ok", "(4).ok")))
  val rec32_al = TestBean3(2, Some(Composite4(1, "x1", Nil, None, "get(\"x1\").ok", "(4).ok")))
  val rec33 = TestBean3(3, Some(Composite4(2, "x2", List("xxx(yyy)zz,z", "u(vv)(w)x(y)", "x=1&y=2&[INSERT_DEVICE_ID_HERE]&z=3"), Some(List("\"t")), "(get(\"A\") + get(\"A\")).ok", "call(A, B).ok")))

  test("Composite type Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        sqlu"create type composite1 as (id int8, txt text, date timestamp, ts_range tsrange)",
        sqlu"create type composite2 as (id int8, comp1 composite1, confirm boolean, map hstore)",
        sqlu"create type composite3 as (txt text, id int4, code int4, bool boolean)",
        sqlu"create type composite4 as (id int4, name text, variable text[], optVars text[], script1 text, script2 text)",
        sqlu"create type c1 as (name text)",
        sqlu"create type c2 as (c1 c1[])",
        (CompositeTests.schema ++ CompositeTests1.schema ++ CompositeTests2.schema ++ CompositeTests3.schema) create,
        CompositeTests forceInsertAll List(rec1, rec2, rec3, rec4),
        CompositeTests1 forceInsertAll List(rec11, rec12, rec13, rec14, rec15),
        CompositeTests2 forceInsertAll List(rec21, rec22),
        CompositeTests3 forceInsertAll List(rec31, rec32, rec33)
      ).andThen(
        DBIO.seq(
          CompositeTests.filter(_.id === 333L.bind).result.head.map(
            r => assert(rec1 === r)
          ),
          CompositeTests.filter(_.id === 335L.bind).result.head.map(
            r => assert(rec2 === r)
          ),
          CompositeTests.filter(_.id === 337L.bind).result.head.map(
            r => assert(rec3 === r)
          ),
          CompositeTests.filter(_.id === 339L.bind).result.head.map(
            r => assert(rec4 === r)
          )
        )
      ).andThen(
        DBIO.seq(
          CompositeTests1.filter(_.id === 111L.bind).result.head.map(
            r => assert(rec11 === r)
          ),
          CompositeTests1.filter(_.id === 112L.bind).result.head.map(
            r => assert(rec12 === r)
          ),
          CompositeTests1.filter(_.id === 113L.bind).result.head.map(
            r => assert(rec13 === r)
          ),
          CompositeTests1.filter(_.id === 114L.bind).result.head.map(
            r => assert(rec14 === r)
          ),
          CompositeTests1.filter(_.id === 115L.bind).result.head.map(
            r => assert(rec15 === r)
          ),
          CompositeTests2.filter(_.comps === rec21.comps.asColumnOf[Composite1]).result.head.map(
            r => assert(rec21 === r)
          )
        )
      ).andThen(
        DBIO.seq(
          CompositeTests2.filter(_.id === 211L.bind).result.head.map(
            r => assert(rec21 === r)
          ),
          CompositeTests2.filter(_.id === 212L.bind).result.head.map(
            r => assert(rec22 === r)
          )
        )
      ).andThen(
        DBIO.seq(
          CompositeTests3.filter(_.id === 1L.bind).result.head.map(r => assert(rec31 === r)),
          CompositeTests3.filter(_.id === 2L.bind).result.head.map(r => assert(rec32_al === r)),
          CompositeTests3.filter(_.id === 3L.bind).result.head.map(r => assert(rec33 === r))
        )
      ).andFinally(
        DBIO.seq(
          (CompositeTests.schema ++ CompositeTests1.schema ++ CompositeTests2.schema ++ CompositeTests3.schema) drop,
          sqlu"drop type c2",
          sqlu"drop type c1",
          sqlu"drop type composite4",
          sqlu"drop type composite3",
          sqlu"drop type composite2",
          sqlu"drop type composite1"
        )
      ).transactionally
    ), Duration.Inf)
  }

  test("Composite type Plain SQL support") {
    import MyPostgresProfile1.plainImplicits._

    implicit val getTestBeanResult: GetResult[TestBean] = GetResult(r => TestBean(r.nextLong(), r.nextArray[Composite2]().toList))
    implicit val getTestBean1Result: GetResult[TestBean1] = GetResult(r => TestBean1(r.nextLong(), r.nextArray[Composite3]().toList))

    Await.result(db.run(DBIO.seq(
      sqlu"create type composite1 as (id int8, txt text, date timestamp, ts_range tsrange)",
      sqlu"create type composite2 as (id int8, comp1 composite1, confirm boolean, map hstore)",
      sqlu"create type composite3 as (txt text, id int4, code int4, bool boolean)",
      sqlu"create table CompositeTest (id BIGINT NOT NULL, comps composite2[] DEFAULT '{}' NOT NULL)",
      sqlu"create table CompositeTest1 (id BIGINT NOT NULL, comps composite3[] NOT NULL)",
      ///
      sqlu"insert into CompositeTest values(${rec1.id}, ${rec1.comps})",
      sql"select * from CompositeTest where id = ${rec1.id}".as[TestBean].head.map(
        r => assert(rec1 === r)
      ),
      sqlu"insert into CompositeTest1 values(${rec11.id}, ${rec11.comps})",
      sql"select * from CompositeTest1 where id = ${rec11.id}".as[TestBean1].head.map(
        r => assert(rec11 === r)
      ),
      ///
      sqlu"drop table CompositeTest1",
      sqlu"drop table CompositeTest",
      sqlu"drop type composite3",
      sqlu"drop type composite2",
      sqlu"drop type composite1"
    ).transactionally), Duration.Inf)
  }
}
