package com.github.tminglei.slickpg

import java.util.UUID
import scala.reflect.ClassTag
import scala.slick.lifted.Column
import scala.slick.driver.PostgresDriver
import java.sql.{Timestamp, Time, Date}
import scala.slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}

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
      def nextShortArray() = nextArray[Int].getOrElse(Nil).map(_.toShort)
      def nextShortArrayOption() = nextArray[Int].map(_.map(_.toShort))
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
      private def nextArray[T](): Option[Seq[T]] = {
        val value = r.rs.getArray(r.skip.currentPos)
        if (r.rs.wasNull) None else Some(
          value.getArray.asInstanceOf[Array[Any]].map(_.asInstanceOf[T]))
      }
    }

    //////////////////////////////////////////////////////////////////////////
    implicit object SetUUIDArray extends SetParameter[Seq[UUID]] {
      def apply(v: Seq[UUID], pp: PositionedParameters) = setArray("uuid", Option(v), pp)
    }
    implicit object SetUUIDArrayOption extends SetParameter[Option[Seq[UUID]]] {
      def apply(v: Option[Seq[UUID]], pp: PositionedParameters) = setArray("uuid", v, pp)
    }
    ///
    implicit object SetStringArray extends SetParameter[Seq[String]] {
      def apply(v: Seq[String], pp: PositionedParameters) = setArray("text", Option(v), pp)
    }
    implicit object SetStringArrayOption extends SetParameter[Option[Seq[String]]] {
      def apply(v: Option[Seq[String]], pp: PositionedParameters) = setArray("text", v, pp)
    }
    ///
    implicit object SetLongArray extends SetParameter[Seq[Long]] {
      def apply(v: Seq[Long], pp: PositionedParameters) = setArray("int8", Option(v), pp)
    }
    implicit object SetLongArrayOption extends SetParameter[Option[Seq[Long]]] {
      def apply(v: Option[Seq[Long]], pp: PositionedParameters) = setArray("int8", v, pp)
    }
    ///
    implicit object SetIntArray extends SetParameter[Seq[Int]] {
      def apply(v: Seq[Int], pp: PositionedParameters) = setArray("int4", Option(v), pp)
    }
    implicit object SetIntArrayOption extends SetParameter[Option[Seq[Int]]] {
      def apply(v: Option[Seq[Int]], pp: PositionedParameters) = setArray("int4", v, pp)
    }
    ///
    implicit object SetShortArray extends SetParameter[Seq[Short]] {
      def apply(v: Seq[Short], pp: PositionedParameters) = setArray("int2", Option(v), pp)
    }
    implicit object SetShortArrayOption extends SetParameter[Option[Seq[Short]]] {
      def apply(v: Option[Seq[Short]], pp: PositionedParameters) = setArray("int2", v, pp)
    }
    ///
    implicit object SetFloatArray extends SetParameter[Seq[Float]] {
      def apply(v: Seq[Float], pp: PositionedParameters) = setArray("float4", Option(v), pp)
    }
    implicit object SetFloatArrayOption extends SetParameter[Option[Seq[Float]]] {
      def apply(v: Option[Seq[Float]], pp: PositionedParameters) = setArray("float4", v, pp)
    }
    ///
    implicit object SetDoubleArray extends SetParameter[Seq[Double]] {
      def apply(v: Seq[Double], pp: PositionedParameters) = setArray("float8", Option(v), pp)
    }
    implicit object SetDoubleArrayOption extends SetParameter[Option[Seq[Double]]] {
      def apply(v: Option[Seq[Double]], pp: PositionedParameters) = setArray("float8", v, pp)
    }
    ///
    implicit object SetBoolArray extends SetParameter[Seq[Boolean]] {
      def apply(v: Seq[Boolean], pp: PositionedParameters) = setArray("bool", Option(v), pp)
    }
    implicit object SetBoolArrayOption extends SetParameter[Option[Seq[Boolean]]] {
      def apply(v: Option[Seq[Boolean]], pp: PositionedParameters) = setArray("bool", v, pp)
    }
    ///
    implicit object SetDateArray extends SetParameter[Seq[Date]] {
      def apply(v: Seq[Date], pp: PositionedParameters) = setArray("date", Option(v), pp)
    }
    implicit object SetDateArrayOption extends SetParameter[Option[Seq[Date]]] {
      def apply(v: Option[Seq[Date]], pp: PositionedParameters) = setArray("date", v, pp)
    }
    ///
    implicit object SetTimeArray extends SetParameter[Seq[Time]] {
      def apply(v: Seq[Time], pp: PositionedParameters) = setArray("time", Option(v), pp)
    }
    implicit object SetTimeArrayOption extends SetParameter[Option[Seq[Time]]] {
      def apply(v: Option[Seq[Time]], pp: PositionedParameters) = setArray("time", v, pp)
    }
    ///
    implicit object SetTimestampArray extends SetParameter[Seq[Timestamp]] {
      def apply(v: Seq[Timestamp], pp: PositionedParameters) = setArray("timestamp", Option(v), pp)
    }
    implicit object SetTimestampArrayOption extends SetParameter[Option[Seq[Timestamp]]] {
      def apply(v: Option[Seq[Timestamp]], pp: PositionedParameters) = setArray("timestamp", v, pp)
    }

    ///
    import utils.SimpleArrayUtils._

    private def setArray[T: ClassTag](baseType: String, v: Option[Seq[T]], p: PositionedParameters) = {
      p.pos += 1
      v match {
        case Some(vList) => p.ps.setArray(p.pos, mkArray(mkString[T](_.toString))(baseType, vList))
        case None        => p.ps.setNull(p.pos, java.sql.Types.ARRAY)
      }
    }
  }
}
