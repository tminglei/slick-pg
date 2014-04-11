package com.github.tminglei.slickpg

import java.util.UUID
import scala.slick.lifted.Column
import scala.slick.driver.PostgresDriver
import java.sql.{Timestamp, Time, Date}

trait PgArraySupport extends array.PgArrayExtensions with array.PgArrayJdbcTypes { driver: PostgresDriver =>
  import utils.TypeConverters.Util._

  trait ArrayImplicits {
    /** for type/name, @see [[org.postgresql.core.Oid]] and [[org.postgresql.jdbc2.TypeInfoCache]]*/
    implicit val uuidListTypeMapper = new ArrayListJdbcType[UUID]("uuid",
      mkArrayConvFromString[UUID], mkArrayConvToString[UUID])
    implicit val strListTypeMapper = new ArrayListJdbcType[String]("text",
      mkArrayConvFromString[String], mkArrayConvToString[String])
    implicit val longListTypeMapper = new ArrayListJdbcType[Long]("int8",
      mkArrayConvFromString[Long], mkArrayConvToString[Long])
    implicit val intListTypeMapper = new ArrayListJdbcType[Int]("int4",
      mkArrayConvFromString[Int], mkArrayConvToString[Int])
    implicit val floatListTypeMapper = new ArrayListJdbcType[Float]("float8",
      mkArrayConvFromString[Float], mkArrayConvToString[Float])
    implicit val boolListTypeMapper = new ArrayListJdbcType[Boolean]("bool",
      mkArrayConvFromString[Boolean], mkArrayConvToString[Boolean])
    implicit val dateListTypeMapper = new ArrayListJdbcType[Date]("date",
      mkArrayConvFromString[Date], mkArrayConvToString[Date])
    implicit val timeListTypeMapper = new ArrayListJdbcType[Time]("time",
      mkArrayConvFromString[Time], mkArrayConvToString[Time])
    implicit val tsListTypeMapper = new ArrayListJdbcType[Timestamp]("timestamp",
      mkArrayConvFromString[Timestamp], mkArrayConvToString[Timestamp])

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
