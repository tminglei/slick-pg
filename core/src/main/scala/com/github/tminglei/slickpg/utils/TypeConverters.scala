package com.github.tminglei.slickpg
package utils

import scala.slick.util.Logging
import scala.reflect.runtime.{universe => u}
import java.sql.{Timestamp, Time, Date}
import java.util.UUID

object TypeConverters extends Logging {
  case class ConvConfig(from: u.Type, to: u.Type, var conv: (_ => _))

  private var convConfigList = List[ConvConfig]()

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
    find(from.tpe, to.tpe).map(_.conv = convert).orElse({
      convConfigList :+= ConvConfig(from.tpe, to.tpe, convert)
      None
    })
  }

  def converter[FROM,TO](implicit from: u.TypeTag[FROM], to: u.TypeTag[TO]): (FROM => TO) = {
    find(from.tpe, to.tpe).map(_.conv.asInstanceOf[(FROM => TO)])
      .getOrElse(throw new IllegalArgumentException(s"Converter NOT FOUND for ${from.tpe} => ${to.tpe}"))
  }

  ///
  private def pgBoolAdjust(s: String): String =
    Option(s).map(_.toLowerCase) match {
      case Some("t")  => "true"
      case Some("f")  => "false"
      case _ => s
    }

  def find(from: u.Type, to: u.Type): Option[ConvConfig] = {
    logger.debug(s"get converter for ${from.erasure} => ${to.erasure}")
    convConfigList.find(e => (e.from =:= from && e.to =:= to)).orElse({
      if (from <:< to) {
        Some(ConvConfig(from, to, (v: Any) => v))
      } else None
    })
  }
}
