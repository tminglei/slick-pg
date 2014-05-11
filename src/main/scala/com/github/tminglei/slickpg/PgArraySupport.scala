package com.github.tminglei.slickpg

import java.util.UUID
import scala.slick.lifted.Column
import scala.slick.driver.PostgresDriver
import java.sql.{Timestamp, Time, Date}
import scala.slick.jdbc.JdbcType

trait PgArraySupport extends array.PgArrayExtensions with array.PgArrayJdbcTypes { driver: PostgresDriver =>

  trait ArrayImplicits {
    /** for type/name, @see [[org.postgresql.core.Oid]] and [[org.postgresql.jdbc2.TypeInfoCache]]*/
    implicit val uuidListTypeMapper = new SimpleArrayListJdbcType[UUID]("uuid")
    implicit val strListTypeMapper = new SimpleArrayListJdbcType[String]("text")
    implicit val longListTypeMapper = new SimpleArrayListJdbcType[Long]("int8")
    implicit val intListTypeMapper = new SimpleArrayListJdbcType[Int]("int4")
    implicit val floatListTypeMapper = new SimpleArrayListJdbcType[Float]("float4")
    implicit val doubleListTypeMapper = new SimpleArrayListJdbcType[Double]("float8")
    implicit val boolListTypeMapper = new SimpleArrayListJdbcType[Boolean]("bool")
    implicit val dateListTypeMapper = new SimpleArrayListJdbcType[Date]("date")
    implicit val timeListTypeMapper = new SimpleArrayListJdbcType[Time]("time")
    implicit val tsListTypeMapper = new SimpleArrayListJdbcType[Timestamp]("timestamp")

    ///
    implicit def arrayColumnExtensionMethods[B1](c: Column[List[B1]])(
      implicit tm: JdbcType[B1], tm1: JdbcType[List[B1]]) = {
        new ArrayColumnExtensionMethods[B1, List[B1]](c)
      }
    implicit def arrayOptionColumnExtensionMethods[B1](c: Column[Option[List[B1]]])(
      implicit tm: JdbcType[B1], tm1: JdbcType[List[B1]]) = {
        new ArrayColumnExtensionMethods[B1, Option[List[B1]]](c)
      }
  }
}
