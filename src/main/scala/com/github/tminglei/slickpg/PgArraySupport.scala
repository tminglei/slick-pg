package com.github.tminglei.slickpg

import java.util.UUID
import java.sql.{Date, Time, Timestamp}
import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}
import scala.reflect.runtime.{universe => u}
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
    implicit val simpleShortListTypeMapper: JdbcType[List[Short]] = new SimpleArrayJdbcType[Short]("int2", _.asInstanceOf[Int].toShort, identity).to(_.toList)
    implicit val simpleFloatListTypeMapper: JdbcType[List[Float]] = new SimpleArrayJdbcType[Float]("float4").to(_.toList)
    implicit val simpleDoubleListTypeMapper: JdbcType[List[Double]] = new SimpleArrayJdbcType[Double]("float8").to(_.toList)
    implicit val simpleBoolListTypeMapper: JdbcType[List[Boolean]] = new SimpleArrayJdbcType[Boolean]("bool").to(_.toList)
    implicit val simpleDateListTypeMapper: JdbcType[List[Date]] = new SimpleArrayJdbcType[Date]("date").to(_.toList)
    implicit val simpleTimeListTypeMapper: JdbcType[List[Time]] = new SimpleArrayJdbcType[Time]("time").to(_.toList)
    implicit val simpleTsListTypeMapper: JdbcType[List[Timestamp]] = new SimpleArrayJdbcType[Timestamp]("timestamp").to(_.toList)

    ///
    implicit def simpleArrayColumnExtensionMethods[B1, SEQ[B1] <: Seq[B1]](c: Rep[SEQ[B1]])(
      implicit tm: JdbcType[B1], tm1: JdbcType[SEQ[B1]]) = {
        new ArrayColumnExtensionMethods[B1, SEQ, SEQ[B1]](c)
      }
    implicit def simpleArrayOptionColumnExtensionMethods[B1, SEQ[B1] <: Seq[B1]](c: Rep[Option[SEQ[B1]]])(
      implicit tm: JdbcType[B1], tm1: JdbcType[SEQ[B1]]) = {
        new ArrayColumnExtensionMethods[B1, SEQ, Option[SEQ[B1]]](c)
      }
  }

  /// static sql support, NOTE: no extension methods available for static sql usage
  trait SimpleArrayPlainImplicits extends SimpleArrayCodeGenSupport {
    import utils.PlainSQLUtils._
    // to support 'nextArray[T]/nextArrayOption[T]' in PgArraySupport
    {
      addNextArrayConverter((r) => simpleNextArray[Int](r).map(_.map(_.toShort)))
    }

    implicit class PgArrayPositionedResult(r: PositionedResult) {
      def nextArray[T]()(implicit tpe: u.TypeTag[T]): Seq[T] = nextArrayOption[T]().getOrElse(Nil)
      def nextArrayOption[T]()(implicit ttag: u.TypeTag[T]): Option[Seq[T]] = {
        nextArrayConverters.get(u.typeOf[T].toString).map(_.apply(r))
          .getOrElse(simpleNextArray[T](r)).asInstanceOf[Option[Seq[T]]]
      }
    }

    private def simpleNextArray[T](r: PositionedResult): Option[Seq[T]] = {
      val value = r.rs.getArray(r.skip.currentPos)
      if (r.rs.wasNull) None else Some(
        value.getArray.asInstanceOf[Array[Any]].map(_.asInstanceOf[T]))
    }

    //////////////////////////////////////////////////////////////////////////
    implicit val getUUIDArray = mkGetResult(_.nextArray[UUID]())
    implicit val getUUIDArrayOption = mkGetResult(_.nextArrayOption[UUID]())
    implicit val setUUIDArray = mkArraySetParameter[UUID]("uuid")
    implicit val setUUIDArrayOption = mkArrayOptionSetParameter[UUID]("uuid")
    ///
    implicit val getStringArray = mkGetResult(_.nextArray[String]())
    implicit val getStringArrayOption = mkGetResult(_.nextArrayOption[String]())
    implicit val setStringArray = mkArraySetParameter[String]("text")
    implicit val setStringArrayOption = mkArrayOptionSetParameter[String]("text")
    ///
    implicit val getLongArray = mkGetResult(_.nextArray[Long]())
    implicit val getLongArrayOption = mkGetResult(_.nextArrayOption[Long]())
    implicit val setLongArray = mkArraySetParameter[Long]("int8")
    implicit val setLongArrayOption = mkArrayOptionSetParameter[Long]("int8")
    ///
    implicit val getIntArray = mkGetResult(_.nextArray[Int]())
    implicit val getIntArrayOption = mkGetResult(_.nextArrayOption[Int]())
    implicit val setIntArray = mkArraySetParameter[Int]("int4")
    implicit val setIntArrayOption = mkArrayOptionSetParameter[Int]("int4")
    ///
    implicit val getShortArray = mkGetResult(_.nextArray[Short]())
    implicit val getShortArrayOption = mkGetResult(_.nextArrayOption[Short]())
    implicit val setShortArray = mkArraySetParameter[Short]("int2")
    implicit val setShortArrayOption = mkArrayOptionSetParameter[Short]("int2")
    ///
    implicit val getFloatArray = mkGetResult(_.nextArray[Float]())
    implicit val getFloatArrayOption = mkGetResult(_.nextArrayOption[Float]())
    implicit val setFloatArray = mkArraySetParameter[Float]("float4")
    implicit val setFloatArrayOption = mkArrayOptionSetParameter[Float]("float4")
    ///
    implicit val getDoubleArray = mkGetResult(_.nextArray[Double]())
    implicit val getDoubleArrayOption = mkGetResult(_.nextArrayOption[Double]())
    implicit val setDoubleArray = mkArraySetParameter[Double]("float8")
    implicit val setDoubleArrayOption = mkArrayOptionSetParameter[Double]("float8")
    ///
    implicit val getBoolArray = mkGetResult(_.nextArray[Boolean]())
    implicit val getBoolArrayOption = mkGetResult(_.nextArrayOption[Boolean]())
    implicit val setBoolArray = mkArraySetParameter[Boolean]("bool")
    implicit val setBoolArrayOption = mkArrayOptionSetParameter[Boolean]("bool")
    ///
    implicit val getDateArray = mkGetResult(_.nextArray[Date]())
    implicit val getDateArrayOption = mkGetResult(_.nextArrayOption[Date]())
    implicit val setDateArray = mkArraySetParameter[Date]("date")
    implicit val setDateArrayOption = mkArrayOptionSetParameter[Date]("date")
    ///
    implicit val getTimeArray = mkGetResult(_.nextArray[Time]())
    implicit val getTimeArrayOption = mkGetResult(_.nextArrayOption[Time]())
    implicit val setTimeArray = mkArraySetParameter[Time]("time")
    implicit val setTimeArrayOption = mkArrayOptionSetParameter[Time]("time")
    ///
    implicit val getTimestampArray = mkGetResult(_.nextArray[Timestamp]())
    implicit val getTimestampArrayOption = mkGetResult(_.nextArrayOption[Timestamp]())
    implicit val setTimestampArray = mkArraySetParameter[Timestamp]("timestamp")
    implicit val setTimestampArrayOption = mkArrayOptionSetParameter[Timestamp]("timestamp")
  }
}
