package com.github.tminglei.slickpg
package utils

import izumi.reflect.{Tag => TTag}
import izumi.reflect.macrortti.LightTypeTag
import slick.util.Logging
import java.sql.{Date, Time, Timestamp}
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID

object TypeConverters extends Logging {
  case class ConvConfig(from: LightTypeTag, to: LightTypeTag, var conv: (_ => _))

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
  register((v: String) => LocalDate.parse(v))
  register((v: String) => LocalTime.parse(v))
  register((v: String) => LocalDateTime.parse(v.replace(' ', 'T')))
  register((v: LocalDate) => v.toString)
  register((v: LocalTime) => v.toString)
  register((v: LocalDateTime) => v.toString)

  def register[FROM,TO](convert: (FROM => TO))(implicit from: TTag[FROM], to: TTag[TO]) = {
    logger.info(s"register converter for ${from.tag.shortName} => ${to.tag.shortName}")
    find(from.tag, to.tag).map(_.conv = convert).orElse({
      convConfigList :+= ConvConfig(from.tag, to.tag, convert)
      None
    })
  }

  def converter[FROM,TO](implicit from: TTag[FROM], to: TTag[TO]): (FROM => TO) = {
    find(from.tag, to.tag).map(_.conv.asInstanceOf[(FROM => TO)])
      .getOrElse(throw new IllegalArgumentException(s"Converter NOT FOUND for ${from.tag} => ${to.tag}"))
  }

  ///
  private def pgBoolAdjust(s: String): String =
    Option(s).map(_.toLowerCase) match {
      case Some("t")  => "true"
      case Some("f")  => "false"
      case _ => s
    }

  def find(from: LightTypeTag, to: LightTypeTag): Option[ConvConfig] = {
    logger.debug(s"get converter for ${from.shortName} => ${to.shortName}")
    convConfigList.find(e => (e.from =:= from && e.to =:= to)).orElse({
      if (from <:< to) {
        Some(ConvConfig(from, to, (v: Any) => v))
      } else None
    })
  }
}
