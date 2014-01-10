package com.github.tminglei.slickpg

import java.util.UUID
import java.sql.{Timestamp, Time, Date}
import scala.slick.lifted.{TypeMapper, Column}
import scala.slick.driver.PostgresDriver

trait PgArraySupport extends array.PgArrayExtensions { driver: PostgresDriver =>
  import array.ArrayTypeMapper
  import utils.TypeConverters.Util._

  trait ArrayImplicits {
    /** for type/name, @see [[org.postgresql.core.Oid]] and [[org.postgresql.jdbc2.TypeInfoCache]]*/
    implicit val uuidListTypeMapper = new ArrayTypeMapper[UUID]("uuid",
        mkArrayConvFromString[UUID], mkArrayConvToString[UUID])
    implicit val strListTypeMapper = new ArrayTypeMapper[String]("text",
        mkArrayConvFromString[String], mkArrayConvToString[String])
    implicit val longListTypeMapper = new ArrayTypeMapper[Long]("int8",
        mkArrayConvFromString[Long], mkArrayConvToString[Long])
    implicit val intListTypeMapper = new ArrayTypeMapper[Int]("int4",
        mkArrayConvFromString[Int], mkArrayConvToString[Int])
    implicit val floatListTypeMapper = new ArrayTypeMapper[Float]("float8",
        mkArrayConvFromString[Float], mkArrayConvToString[Float])
    implicit val boolListTypeMapper = new ArrayTypeMapper[Boolean]("bool",
        mkArrayConvFromString[Boolean], mkArrayConvToString[Boolean])
    implicit val dateListTypeMapper = new ArrayTypeMapper[Date]("date",
        mkArrayConvFromString[Date], mkArrayConvToString[Date])
    implicit val timeListTypeMapper = new ArrayTypeMapper[Time]("time",
        mkArrayConvFromString[Time], mkArrayConvToString[Time])
    implicit val tsListTypeMapper = new ArrayTypeMapper[Timestamp]("timestamp",
        mkArrayConvFromString[Timestamp], mkArrayConvToString[Timestamp])

    ///
    implicit def arrayColumnExtensionMethods[B1](c: Column[List[B1]])(
      implicit tm: TypeMapper[B1], tm1: ArrayTypeMapper[B1]) = {
        new ArrayColumnExtensionMethods[B1, List[B1]](c)
      }
    implicit def arrayOptionColumnExtensionMethods[B1](c: Column[Option[List[B1]]])(
      implicit tm: TypeMapper[B1], tm1: ArrayTypeMapper[B1]) = {
        new ArrayColumnExtensionMethods[B1, Option[List[B1]]](c)
      }
  }
}
