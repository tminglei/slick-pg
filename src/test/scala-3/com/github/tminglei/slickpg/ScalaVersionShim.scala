package com.github.tminglei.slickpg

import com.github.tminglei.slickpg.utils.{RegisteredTypeConverter,TypeConverters}

import org.postgresql.util.HStoreConverter

import java.time.LocalDateTime
import scala.jdk.CollectionConverters._

object ScalaVersionShim {
  def ts(str: String) = LocalDateTime.parse(str.replace(' ', 'T'))
  def mapToString(m: Map[String, String]): String = HStoreConverter.toString((m).asJava)
  def stringToMap(s: String): Map[String, String] = (HStoreConverter.fromString(s)
    .asInstanceOf[java.util.Map[String, String]]).asScala.toMap
  implicit val StringToRange: RegisteredTypeConverter[String, Range[LocalDateTime]] =
    RegisteredTypeConverter(PgRangeSupportUtils.mkRangeFn(ts))
  implicit val RangeToString: RegisteredTypeConverter[Range[LocalDateTime], String] =
    RegisteredTypeConverter(PgRangeSupportUtils.toStringFn[LocalDateTime](_.toString))
  implicit val MapToString: RegisteredTypeConverter[Map[String, String], String] = RegisteredTypeConverter(mapToString)
  implicit val StringToMap: RegisteredTypeConverter[String, Map[String, String]] = RegisteredTypeConverter(stringToMap)

  def registerTypeConverters(): Unit = ()
  val maybeTypeConverters: RegisteredTypeConverter.type = RegisteredTypeConverter
}
