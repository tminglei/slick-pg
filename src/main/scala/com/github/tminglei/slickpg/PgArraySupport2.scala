package com.github.tminglei.slickpg

import java.util.UUID
import scala.slick.lifted.{TypeMapper, Column}
import scala.slick.driver.PostgresDriver

trait PgArraySupport2 { driver: PostgresDriver =>
  import array._

  val arrayExt = new array.PgArrayExtensions {
    type LIST[T] = List[T]
  }

  trait ArrayImplicits {
    /** for type/name, @see [[org.postgresql.core.Oid]] and [[org.postgresql.jdbc2.TypeInfoCache]]*/
    implicit val uuidListTypeMapper = new ArrayTypeMapper[UUID]("uuid")
    implicit val strListTypeMapper = new ArrayTypeMapper[String]("text")
    implicit val longListTypeMapper = new ArrayTypeMapper[Long]("int8")
    implicit val intListTypeMapper = new ArrayTypeMapper[Int]("int4")
    implicit val floatListTypeMapper = new ArrayTypeMapper[Float]("float8")
    implicit val boolListTypeMapper = new ArrayTypeMapper[Boolean]("bool")
    implicit val dateListTypeMapper = new ArrayTypeMapper[java.sql.Date]("date")
    implicit val timeListTypeMapper = new ArrayTypeMapper[java.sql.Time]("time")
    implicit val tsListTypeMapper = new ArrayTypeMapper[java.sql.Timestamp]("timestamp")

    ///
    implicit def arrayColumnExtensionMethods[B1](c: Column[List[B1]])(
      implicit tm: TypeMapper[B1], tm1: ArrayTypeMapper[B1]) = {
        new arrayExt.ArrayColumnExtensionMethods[B1, List[B1]](c)
      }
    implicit def arrayOptionColumnExtensionMethods[B1](c: Column[Option[List[B1]]])(
      implicit tm: TypeMapper[B1], tm1: ArrayTypeMapper[B1]) = {
        new arrayExt.ArrayColumnExtensionMethods[B1, Option[List[B1]]](c)
      }
  }
}
