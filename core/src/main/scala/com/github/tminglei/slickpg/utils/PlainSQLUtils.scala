package com.github.tminglei.slickpg.utils

import slick.util.Logging

import izumi.reflect.{Tag => TTag}
import scala.reflect.ClassTag
import slick.jdbc.{GetResult, PositionedResult, SetParameter, PositionedParameters}

object PlainSQLUtils extends Logging {
  import SimpleArrayUtils._
  private[slickpg] var nextArrayConverters = Map.empty[String, PositionedResult => Option[Seq[_]]]

  /** used to support 'nextArray[T]/nextArrayOption[T]' in PgArraySupport */
  def addNextArrayConverter[T](conv: PositionedResult => Option[Seq[T]])(implicit ttag: TTag[T]) = {
    logger.info(s"\u001B[36m >>> adding next array converter for ${ttag.tag.shortName} \u001B[0m")
      val convKey = ttag.tag.shortName
      val existed = nextArrayConverters.get(convKey)
      if (existed.isDefined) logger.warn(
        s"\u001B[31m >>> DUPLICATED next array converter for ${ttag.tag.shortName}!!! \u001B[36m If it's expected, pls ignore it.\u001B[0m"
      )
      nextArrayConverters += (convKey -> conv)
  }

  ///
  def mkGetResult[T](next: (PositionedResult => T)) =
    new GetResult[T] {
      def apply(rs: PositionedResult) = next(rs)
    }

  ///
  def mkSetParameter[T](typeName: String, toStr: (T => String) = (v: T) => v.toString,
            sqlType: Int = java.sql.Types.OTHER): SetParameter[T] =
    new SetParameter[T] {
      def apply(v: T, pp: PositionedParameters) = internalSet(sqlType, typeName, Option(v), pp, toStr)
    }
  def mkOptionSetParameter[T](typeName: String, toStr: (T => String) = (v: T) => v.toString,
            sqlType: Int = java.sql.Types.OTHER): SetParameter[Option[T]] =
    new SetParameter[Option[T]] {
      def apply(v: Option[T], pp: PositionedParameters) = internalSet(sqlType, typeName, v, pp, toStr)
    }

  private def internalSet[T](sqlType: Int, typeName: String, v: Option[T], p: PositionedParameters, toStr: (T => String)) =
    v match {
      case Some(v) => p.setObject(toStr(v), java.sql.Types.OTHER)
      case None    => p.setNull(sqlType)
    }

  ///
  def mkArraySetParameter[T: ClassTag](baseType: String, toStr: (T => String) = (v: T) => v.toString,
          seqToStr: Option[(Seq[T] => String)] = None): SetParameter[Seq[T]] =
    new SetParameter[Seq[T]] {
      def apply(v: Seq[T], pp: PositionedParameters) = internalSetArray(baseType, Option(v), pp, toStr, seqToStr)
    }
  def mkArrayOptionSetParameter[T: ClassTag](baseType: String, toStr: (T => String) = (v: T) => v.toString,
          seqToStr: Option[(Seq[T] => String)] = None): SetParameter[Option[Seq[T]]] =
    new SetParameter[Option[Seq[T]]] {
      def apply(v: Option[Seq[T]], pp: PositionedParameters) = internalSetArray(baseType, v, pp, toStr, seqToStr)
    }

  private def internalSetArray[T: ClassTag](baseType: String, v: Option[Seq[T]], p: PositionedParameters,
          toStr: (T => String), seqToStr: Option[(Seq[T] => String)]) = {
    val _seqToStr = seqToStr.getOrElse(mkStringUnsafe(toStr) _); p.pos += 1
    v match {
      case Some(vList) => p.ps.setArray(p.pos, mkArray(_seqToStr)(baseType, vList))
      case None        => p.ps.setNull(p.pos, java.sql.Types.ARRAY)
    }
  }
}
