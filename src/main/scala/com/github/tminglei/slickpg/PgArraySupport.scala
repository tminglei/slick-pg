package com.github.tminglei.slickpg

import java.util.UUID
import scala.slick.lifted.Column
import scala.slick.driver.PostgresDriver
import java.sql.{Timestamp, Time, Date}
import scala.slick.jdbc.JdbcType

trait PgArraySupport extends array.PgArrayExtensions with array.PgArrayJdbcTypes { driver: PostgresDriver =>

  /// alias
  trait ArrayImplicits extends SimpleArrayImplicits

  trait SimpleArrayImplicits {
    /** for type/name, @see [[org.postgresql.core.Oid]] and [[org.postgresql.jdbc2.TypeInfoCache]]*/
    implicit val simpleUUIDListTypeMapper = new SimpleArrayListJdbcType[UUID]("uuid")
    implicit val simpleStrListTypeMapper = new SimpleArrayListJdbcType[String]("text")
    implicit val simpleLongListTypeMapper = new SimpleArrayListJdbcType[Long]("int8")
    implicit val simpleIntListTypeMapper = new SimpleArrayListJdbcType[Int]("int4")
    implicit val simpleFloatListTypeMapper = new SimpleArrayListJdbcType[Float]("float4")
    implicit val simpleDoubleListTypeMapper = new SimpleArrayListJdbcType[Double]("float8")
    implicit val simpleBoolListTypeMapper = new SimpleArrayListJdbcType[Boolean]("bool")
    implicit val simpleDateListTypeMapper = new SimpleArrayListJdbcType[Date]("date")
    implicit val simpleTimeListTypeMapper = new SimpleArrayListJdbcType[Time]("time")
    implicit val simpleTsListTypeMapper = new SimpleArrayListJdbcType[Timestamp]("timestamp")

    ///
    implicit def simpleArrayColumnExtensionMethods[B1](c: Column[List[B1]])(
      implicit tm: JdbcType[B1], tm1: JdbcType[List[B1]]) = {
        new ArrayColumnExtensionMethods[B1, List[B1]](c)
      }
    implicit def simpleArrayOptionColumnExtensionMethods[B1](c: Column[Option[List[B1]]])(
      implicit tm: JdbcType[B1], tm1: JdbcType[List[B1]]) = {
        new ArrayColumnExtensionMethods[B1, Option[List[B1]]](c)
      }
  }
}
