package com.github.tminglei.slickpg

import izumi.reflect.{Tag => TTag}
import java.util.UUID
import java.sql.{Date, Time, Timestamp}
import slick.jdbc.{GetResult, JdbcType, PositionedResult, PostgresProfile, SetParameter}
import scala.reflect.classTag

trait PgArraySupport extends array.PgArrayExtensions with array.PgArrayJdbcTypes { driver: PostgresProfile =>
  import driver.api._

  trait SimpleArrayCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_uuid", classTag[Seq[UUID]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_text", classTag[Seq[String]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_int8", classTag[Seq[Long]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_int4", classTag[Seq[Int]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_int2", classTag[Seq[Short]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_float4", classTag[Seq[Float]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_float8", classTag[Seq[Double]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_bool", classTag[Seq[Boolean]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_date", classTag[Seq[Date]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_time", classTag[Seq[Time]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_timestamp", classTag[Seq[Timestamp]])
    }
  }

  /// alias
  trait ArrayImplicits extends SimpleArrayImplicits

  trait SimpleArrayImplicits extends SimpleArrayCodeGenSupport {
    /** for type/name, @see [[org.postgresql.core.Oid]] and [[org.postgresql.jdbc.TypeInfoCache]]*/
    implicit val simpleUUIDListTypeMapper: JdbcType[List[UUID]] = new SimpleArrayJdbcType[UUID]("uuid").to(_.toList)
    implicit val simpleStrListTypeMapper: JdbcType[List[String]] = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val simpleLongListTypeMapper: JdbcType[List[Long]] = new SimpleArrayJdbcType[Long]("int8").to(_.toList)
    implicit val simpleIntListTypeMapper: JdbcType[List[Int]] = new SimpleArrayJdbcType[Int]("int4").to(_.toList)
    implicit val simpleShortListTypeMapper: JdbcType[List[Short]] = new SimpleArrayJdbcType[Short]("int2").to(_.toList)
    implicit val simpleFloatListTypeMapper: JdbcType[List[Float]] = new SimpleArrayJdbcType[Float]("float4").to(_.toList)
    implicit val simpleDoubleListTypeMapper: JdbcType[List[Double]] = new SimpleArrayJdbcType[Double]("float8").to(_.toList)
    implicit val simpleBoolListTypeMapper: JdbcType[List[Boolean]] = new SimpleArrayJdbcType[Boolean]("bool").to(_.toList)
    implicit val simpleDateListTypeMapper: JdbcType[List[Date]] = new SimpleArrayJdbcType[Date]("date").to(_.toList)
    implicit val simpleTimeListTypeMapper: JdbcType[List[Time]] = new SimpleArrayJdbcType[Time]("time").to(_.toList)
    implicit val simpleTsListTypeMapper: JdbcType[List[Timestamp]] = new SimpleArrayJdbcType[Timestamp]("timestamp").to(_.toList)

    ///
    implicit def simpleArrayColumnExtensionMethods[B1, SEQ[B1]](c: Rep[SEQ[B1]])(
      implicit tm: JdbcType[B1], tm1: JdbcType[SEQ[B1]]): ArrayColumnExtensionMethods[B1, SEQ, SEQ[B1]] = {
        new ArrayColumnExtensionMethods[B1, SEQ, SEQ[B1]](c)
      }
    implicit def simpleArrayOptionColumnExtensionMethods[B1, SEQ[B1]](c: Rep[Option[SEQ[B1]]])(
      implicit tm: JdbcType[B1], tm1: JdbcType[SEQ[B1]]): ArrayColumnExtensionMethods[B1, SEQ, Option[SEQ[B1]]] = {
        new ArrayColumnExtensionMethods[B1, SEQ, Option[SEQ[B1]]](c)
      }
  }

  /// static sql support, NOTE: no extension methods available for static sql usage
  trait SimpleArrayPlainImplicits extends SimpleArrayCodeGenSupport {
    import utils.PlainSQLUtils._
    // to support 'nextArray[T]/nextArrayOption[T]' in PgArraySupport
    {
//      addNextArrayConverter((r) => simpleNextArray[Int](r).map(_.map(_.toShort)))
    }

    implicit class PgArrayPositionedResult(r: PositionedResult) {
      def nextArray[T]()(implicit tpe: TTag[T]): Seq[T] = nextArrayOption[T]().getOrElse(Nil)
      def nextArrayOption[T]()(implicit ttag: TTag[T]): Option[Seq[T]] = {
        nextArrayConverters.get(ttag.tag.shortName).map(_.apply(r))
          .getOrElse(simpleNextArray[T](r)).asInstanceOf[Option[Seq[T]]]
      }
    }

    private def simpleNextArray[T](r: PositionedResult): Option[Seq[T]] = {
      val value = r.rs.getArray(r.skip.currentPos)
      if (r.rs.wasNull) None else Some(
        value.getArray.asInstanceOf[Array[Any]].toSeq.map(_.asInstanceOf[T]))
    }

    //////////////////////////////////////////////////////////////////////////
    implicit val getUUIDArray: GetResult[Seq[UUID]] = mkGetResult(_.nextArray[UUID]())
    implicit val getUUIDArrayOption: GetResult[Option[Seq[UUID]]] = mkGetResult(_.nextArrayOption[UUID]())
    implicit val setUUIDArray: SetParameter[Seq[UUID]] = mkArraySetParameter[UUID]("uuid")
    implicit val setUUIDArrayOption: SetParameter[Option[Seq[UUID]]] = mkArrayOptionSetParameter[UUID]("uuid")
    ///
    implicit val getStringArray: GetResult[Seq[String]] = mkGetResult(_.nextArray[String]())
    implicit val getStringArrayOption: GetResult[Option[Seq[String]]] = mkGetResult(_.nextArrayOption[String]())
    implicit val setStringArray: SetParameter[Seq[String]] = mkArraySetParameter[String]("text")
    implicit val setStringArrayOption: SetParameter[Option[Seq[String]]] = mkArrayOptionSetParameter[String]("text")
    ///
    implicit val getLongArray: GetResult[Seq[Long]] = mkGetResult(_.nextArray[Long]())
    implicit val getLongArrayOption: GetResult[Option[Seq[Long]]] = mkGetResult(_.nextArrayOption[Long]())
    implicit val setLongArray: SetParameter[Seq[Long]] = mkArraySetParameter[Long]("int8")
    implicit val setLongArrayOption: SetParameter[Option[Seq[Long]]] = mkArrayOptionSetParameter[Long]("int8")
    ///
    implicit val getIntArray: GetResult[Seq[Int]] = mkGetResult(_.nextArray[Int]())
    implicit val getIntArrayOption: GetResult[Option[Seq[Int]]] = mkGetResult(_.nextArrayOption[Int]())
    implicit val setIntArray: SetParameter[Seq[Int]] = mkArraySetParameter[Int]("int4")
    implicit val setIntArrayOption: SetParameter[Option[Seq[Int]]] = mkArrayOptionSetParameter[Int]("int4")
    ///
    implicit val getShortArray: GetResult[Seq[Short]] = mkGetResult(_.nextArray[Short]())
    implicit val getShortArrayOption: GetResult[Option[Seq[Short]]] = mkGetResult(_.nextArrayOption[Short]())
    implicit val setShortArray: SetParameter[Seq[Short]] = mkArraySetParameter[Short]("int2")
    implicit val setShortArrayOption: SetParameter[Option[Seq[Short]]] = mkArrayOptionSetParameter[Short]("int2")
    ///
    implicit val getFloatArray: GetResult[Seq[Float]] = mkGetResult(_.nextArray[Float]())
    implicit val getFloatArrayOption: GetResult[Option[Seq[Float]]] = mkGetResult(_.nextArrayOption[Float]())
    implicit val setFloatArray: SetParameter[Seq[Float]] = mkArraySetParameter[Float]("float4")
    implicit val setFloatArrayOption: SetParameter[Option[Seq[Float]]] = mkArrayOptionSetParameter[Float]("float4")
    ///
    implicit val getDoubleArray: GetResult[Seq[Double]] = mkGetResult(_.nextArray[Double]())
    implicit val getDoubleArrayOption: GetResult[Option[Seq[Double]]] = mkGetResult(_.nextArrayOption[Double]())
    implicit val setDoubleArray: SetParameter[Seq[Double]] = mkArraySetParameter[Double]("float8")
    implicit val setDoubleArrayOption: SetParameter[Option[Seq[Double]]] = mkArrayOptionSetParameter[Double]("float8")
    ///
    implicit val getBoolArray: GetResult[Seq[Boolean]] = mkGetResult(_.nextArray[Boolean]())
    implicit val getBoolArrayOption: GetResult[Option[Seq[Boolean]]] = mkGetResult(_.nextArrayOption[Boolean]())
    implicit val setBoolArray: SetParameter[Seq[Boolean]] = mkArraySetParameter[Boolean]("bool")
    implicit val setBoolArrayOption: SetParameter[Option[Seq[Boolean]]] = mkArrayOptionSetParameter[Boolean]("bool")
    ///
    implicit val getDateArray: GetResult[Seq[Date]] = mkGetResult(_.nextArray[Date]())
    implicit val getDateArrayOption: GetResult[Option[Seq[Date]]] = mkGetResult(_.nextArrayOption[Date]())
    implicit val setDateArray: SetParameter[Seq[Date]] = mkArraySetParameter[Date]("date")
    implicit val setDateArrayOption: SetParameter[Option[Seq[Date]]] = mkArrayOptionSetParameter[Date]("date")
    ///
    implicit val getTimeArray: GetResult[Seq[Time]] = mkGetResult(_.nextArray[Time]())
    implicit val getTimeArrayOption: GetResult[Option[Seq[Time]]] = mkGetResult(_.nextArrayOption[Time]())
    implicit val setTimeArray: SetParameter[Seq[Time]] = mkArraySetParameter[Time]("time")
    implicit val setTimeArrayOption: SetParameter[Option[Seq[Time]]] = mkArrayOptionSetParameter[Time]("time")
    ///
    implicit val getTimestampArray: GetResult[Seq[Timestamp]] = mkGetResult(_.nextArray[Timestamp]())
    implicit val getTimestampArrayOption: GetResult[Option[Seq[Timestamp]]] = mkGetResult(_.nextArrayOption[Timestamp]())
    implicit val setTimestampArray: SetParameter[Seq[Timestamp]] = mkArraySetParameter[Timestamp]("timestamp")
    implicit val setTimestampArrayOption: SetParameter[Option[Seq[Timestamp]]] = mkArrayOptionSetParameter[Timestamp]("timestamp")
  }
}
