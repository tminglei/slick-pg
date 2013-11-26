package com.github.tminglei.slickpg

import java.util.UUID
import scala.slick.lifted.Column
import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.JdbcType

trait PgArraySupport extends array.PgArrayExtensions { driver: PostgresDriver =>
  import array.ArrayListJavaType

  trait ArrayImplicits {
    /** for type/name, @see [[org.postgresql.core.Oid]] and [[org.postgresql.jdbc2.TypeInfoCache]]*/
    implicit val uuidListTypeMapper = new ArrayListJavaType[UUID]("uuid")
    implicit val strListTypeMapper = new ArrayListJavaType[String]("text")
    implicit val longListTypeMapper = new ArrayListJavaType[Long]("int8")
    implicit val intListTypeMapper = new ArrayListJavaType[Int]("int4")
    implicit val floatListTypeMapper = new ArrayListJavaType[Float]("float8")
    implicit val boolListTypeMapper = new ArrayListJavaType[Boolean]("bool")
    implicit val dateListTypeMapper = new ArrayListJavaType[java.sql.Date]("date")
    implicit val timeListTypeMapper = new ArrayListJavaType[java.sql.Time]("time")
    implicit val tsListTypeMapper = new ArrayListJavaType[java.sql.Timestamp]("timestamp")

    ///
    implicit def arrayColumnExtensionMethods[B1](c: Column[List[B1]])(
      implicit tm: JdbcType[B1], tm1: ArrayListJavaType[B1]) = {
        new ArrayColumnExtensionMethods[B1, List[B1]](c)
      }
    implicit def arrayOptionColumnExtensionMethods[B1](c: Column[Option[List[B1]]])(
      implicit tm: JdbcType[B1], tm1: ArrayListJavaType[B1]) = {
        new ArrayColumnExtensionMethods[B1, Option[List[B1]]](c)
      }
  }
}
