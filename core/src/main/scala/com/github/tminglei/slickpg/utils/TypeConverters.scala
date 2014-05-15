package com.github.tminglei.slickpg
package utils

import scala.slick.util.Logging
import scala.reflect.runtime.{universe => u}
import java.sql.{Timestamp, Time, Date}
import java.util.UUID

object TypeConverters extends Logging {
  private var converterMap = Map[Key, (_ => _)]()

  // register basic converters
  register((v: String) => v.toInt)
  register((v: String) => v.toLong)
  register((v: String) => v.toShort)
  register((v: String) => v.toFloat)
  register((v: String) => v.toDouble)
  register((v: String) => pgBoolAdjust(v).toBoolean)
  register((v: String) => v.toByte)
  register((v: String) => UUID.fromString(v))
  // register date/time converters
  register((v: String) => Date.valueOf(v))
  register((v: String) => Time.valueOf(v))
  register((v: String) => Timestamp.valueOf(v))
  register((v: Date) => v.toString)
  register((v: Time) => v.toString)
  register((v: Timestamp) => v.toString)

  def register[FROM,TO](convert: (FROM => TO))(implicit from: u.TypeTag[FROM], to: u.TypeTag[TO]) = {
    logger.info(s"register converter for ${from.tpe.erasure} => ${to.tpe.erasure}")
    converterMap += (Key(from.tpe, to.tpe) -> convert)
  }

  def converter[FROM,TO](implicit from: u.TypeTag[FROM], to: u.TypeTag[TO]): (FROM => TO) = {
    find(from.tpe, to.tpe).map(_.asInstanceOf[(FROM => TO)])
      .getOrElse(throw new IllegalArgumentException(s"Converter NOT FOUND for ${from.tpe} => ${to.tpe}"))
  }

  ///
  private def pgBoolAdjust(s: String): String =
    Option(s).map(_.toLowerCase) match {
      case Some("t")  => "true"
      case Some("f")  => "false"
      case _ => s
    }

  def find(from: u.Type, to: u.Type) = {
    val cacheKey = Key(from, to)
    logger.debug(s"get converter for ${from.erasure} => ${to.erasure}")
    converterMap.get(cacheKey).orElse({
      if (to <:< from) {
        converterMap += (cacheKey -> ((v: Any) => v))
        converterMap.get(cacheKey)
      } else None
    })
  }

  ////////////////////////////////////////////////////////////////////////////
  private[utils] case class Key(val from: u.Type, val to: u.Type) {
    override def hashCode(): Int = {
      from.erasure.hashCode() * 31 + to.erasure.hashCode()
    }
    override def equals(o: Any) = {
      if (o.isInstanceOf[Key]) {
        val that = o.asInstanceOf[Key]
        from =:= that.from && to =:= that.to
      } else false
    }
  }
}
