package com.github.tminglei.slickpg

import java.util.UUID
import scala.reflect.ClassTag
import scala.slick.lifted.Column
import scala.slick.driver.PostgresDriver
import java.sql.{Timestamp, Time, Date}
import scala.slick.jdbc.{PositionedParameters, PositionedResult, JdbcType}

trait PgArraySupport extends array.PgArrayExtensions with array.PgArrayJdbcTypes { driver: PostgresDriver =>

  /// alias
  trait ArrayImplicits extends SimpleArrayImplicits

  trait SimpleArrayImplicits {
    /** for type/name, @see [[org.postgresql.core.Oid]] and [[org.postgresql.jdbc2.TypeInfoCache]]*/
    implicit val simpleUUIDListTypeMapper = new SimpleArrayJdbcType[UUID]("uuid").to(_.toList)
    implicit val simpleStrListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val simpleLongListTypeMapper = new SimpleArrayJdbcType[Long]("int8").to(_.toList)
    implicit val simpleIntListTypeMapper = new SimpleArrayJdbcType[Int]("int4").to(_.toList)
    implicit val simpleShortListTypeMapper = new SimpleArrayJdbcType[Short]("int2").to(_.toList)
    implicit val simpleFloatListTypeMapper = new SimpleArrayJdbcType[Float]("float4").to(_.toList)
    implicit val simpleDoubleListTypeMapper = new SimpleArrayJdbcType[Double]("float8").to(_.toList)
    implicit val simpleBoolListTypeMapper = new SimpleArrayJdbcType[Boolean]("bool").to(_.toList)
    implicit val simpleDateListTypeMapper = new SimpleArrayJdbcType[Date]("date").to(_.toList)
    implicit val simpleTimeListTypeMapper = new SimpleArrayJdbcType[Time]("time").to(_.toList)
    implicit val simpleTsListTypeMapper = new SimpleArrayJdbcType[Timestamp]("timestamp").to(_.toList)

    ///
    implicit def simpleArrayColumnExtensionMethods[B1, SEQ[B1] <: Seq[B1]](c: Column[SEQ[B1]])(
      implicit tm: JdbcType[B1], tm1: JdbcType[SEQ[B1]]) = {
        new ArrayColumnExtensionMethods[B1, SEQ, SEQ[B1]](c)
      }
    implicit def simpleArrayOptionColumnExtensionMethods[B1, SEQ[B1] <: Seq[B1]](c: Column[Option[SEQ[B1]]])(
      implicit tm: JdbcType[B1], tm1: JdbcType[SEQ[B1]]) = {
        new ArrayColumnExtensionMethods[B1, SEQ, Option[SEQ[B1]]](c)
      }
  }

  /// static sql support, NOTE: no extension methods available for static sql usage
  trait SimpleArrayPlainImplicits {
    implicit class PgArrayPositionedResult(r: PositionedResult) {
      // uuid array
      def nextUUIDArray() = nextArray[UUID].getOrElse(Nil)
      def nextUUIDArrayOption() = nextArray[UUID]
      // string array
      def nextStringArray() = nextArray[String].getOrElse(Nil)
      def nextStringArrayOption() = nextArray[String]
      // long array
      def nextLongArray() = nextArray[Long].getOrElse(Nil)
      def nextLongArrayOption() = nextArray[Long]
      // int array
      def nextIntArray() = nextArray[Int].getOrElse(Nil)
      def nextIntArrayOption() = nextArray[Int]
      // short array
      def nextShortArray() = nextArray[Short].getOrElse(Nil)
      def nextShortArrayOption() = nextArray[Short]
      // float array
      def nextFloatArray() = nextArray[Float].getOrElse(Nil)
      def nextFloatArrayOption() = nextArray[Float]
      // double array
      def nextDoubleArray() = nextArray[Double].getOrElse(Nil)
      def nextDoubleArrayOption() = nextArray[Double]
      // boolean array
      def nextBooleanArray() = nextArray[Boolean].getOrElse(Nil)
      def nextBooleanArrayOption() = nextArray[Boolean]
      // date array
      def nextDateArray() = nextArray[Date].getOrElse(Nil)
      def nextDateArrayOption() = nextArray[Date]
      // time array
      def nextTimeArray() = nextArray[Time].getOrElse(Nil)
      def nextTimeArrayOption() = nextArray[Time]
      // timestamp array
      def nextTimestampArray() = nextArray[Timestamp].getOrElse(Nil)
      def nextTimestampArrayOption() = nextArray[Timestamp]

      ///
      private def nextArray[T](): Option[List[T]] = {
        val value = r.rs.getArray(r.skip.currentPos)
        if (r.rs.wasNull) None else Some(
          value.getArray.asInstanceOf[Array[Any]].map(_.asInstanceOf[T]).toList)
      }
    }

    implicit class PgArrayPositionedParameters(p: PositionedParameters) {
      import utils.SimpleArrayUtils._
      // uuid array
      def setUUIDArray(v: List[UUID]) = setArray("uuid", Option(v))
      def setUUIDArrayOption(v: Option[List[UUID]]) = setArray("uuid", v)
      // string array
      def setStringArray(v: List[String]) = setArray("text", Option(v))
      def setStringArrayOption(v: Option[List[String]]) = setArray("text", v)
      // long array
      def setLongArray(v: List[String]) = setArray("int8", Option(v))
      def setLongArrayOption(v: Option[List[String]]) = setArray("int8", v)
      // int array
      def setIntArray(v: List[Int]) = setArray("int4", Option(v))
      def setIntArrayOption(v: Option[List[Int]]) = setArray("int4", v)
      // short array
      def setShortArray(v: List[Short]) = setArray("int2", Option(v))
      def setShortArrayOption(v: Option[List[Short]]) = setArray("int2", v)
      // float array
      def setFloatArray(v: List[Float]) = setArray("float4", Option(v))
      def setFloatArrayOption(v: Option[List[Float]]) = setArray("float8", v)
      // double array
      def setDoubleArray(v: List[Double]) = setArray("float8", Option(v))
      def setDoubleArrayOption(v: Option[List[Double]]) = setArray("float8", v)
      // boolean array
      def setBooleanArray(v: List[Boolean]) = setArray("bool", Option(v))
      def setBooleanArrayOption(v: Option[List[Boolean]]) = setArray("bool", v)
      // date array
      def setDateArray(v: List[Date]) = setArray("date", Option(v))
      def setDateArrayOption(v: Option[List[Date]]) = setArray("date", v)
      // time array
      def setTimeArray(v: List[Time]) = setArray("time", Option(v))
      def setTimeArrayOption(v: Option[List[Time]]) = setArray("time", v)
      // timestamp array
      def setTimestampArray(v: List[Timestamp]) = setArray("timestamp", Option(v))
      def setTimestampArrayOption(v: Option[List[Timestamp]]) = setArray("timestamp", v)

      ///
      private def setArray[T: ClassTag](baseType: String, v: Option[List[T]]) = {
        p.pos += 1
        v match {
          case Some(vList) => p.ps.setArray(p.pos, mkArray(mkString[T](_.toString))(baseType, vList))
          case None        => p.ps.setNull(p.pos, java.sql.Types.ARRAY)
        }
      }
    }
  }
}
