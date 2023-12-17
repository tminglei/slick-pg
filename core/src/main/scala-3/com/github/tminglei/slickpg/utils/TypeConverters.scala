package com.github.tminglei.slickpg
package utils

import izumi.reflect.{Tag => TTag}
import izumi.reflect.macrortti.LightTypeTag
import slick.util.Logging
import java.sql.{Date, Time, Timestamp}
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID

case class RegisteredTypeConverter[From, To](convert: From => To)

object TypeConverters extends Logging {
  case class ConvConfig(from: LightTypeTag, to: LightTypeTag, var conv: (_ => _))

  private var convConfigList = List[ConvConfig]()

  // register basic converters
  implicit val StringToInt: RegisteredTypeConverter[String, Int] = RegisteredTypeConverter[String, Int](_.toInt)
  implicit val StringToLong: RegisteredTypeConverter[String, Long] = RegisteredTypeConverter(_.toLong)
  implicit val StringToShort: RegisteredTypeConverter[String, Short] = RegisteredTypeConverter(_.toShort)
  implicit val StringToFloat: RegisteredTypeConverter[String, Float] = RegisteredTypeConverter(_.toFloat)
  implicit val StringToDouble: RegisteredTypeConverter[String, Double] = RegisteredTypeConverter(_.toDouble)
  implicit val StringToBoolean: RegisteredTypeConverter[String, Boolean] = RegisteredTypeConverter(pgBoolAdjust(_).toBoolean)
  implicit val StringToByte: RegisteredTypeConverter[String, Byte] = RegisteredTypeConverter(_.toByte)
  implicit val StringToUUID: RegisteredTypeConverter[String, UUID] = RegisteredTypeConverter(UUID.fromString)
  implicit val IntToString: RegisteredTypeConverter[Int, String] = RegisteredTypeConverter(_.toString)
  implicit val LongToString: RegisteredTypeConverter[Long, String] = RegisteredTypeConverter(_.toString)
  implicit val ShortToString: RegisteredTypeConverter[Short, String] = RegisteredTypeConverter(_.toString)
  implicit val FloatToString: RegisteredTypeConverter[Float, String] = RegisteredTypeConverter(_.toString)
  implicit val DoubleToString: RegisteredTypeConverter[Double, String] = RegisteredTypeConverter(_.toString)
  implicit val BooleanToString: RegisteredTypeConverter[Boolean, String] = RegisteredTypeConverter(_.toString.take(1))
  implicit val ByteToString: RegisteredTypeConverter[Byte, String] = RegisteredTypeConverter(_.toString)
  implicit val UUIDToString: RegisteredTypeConverter[UUID, String] = RegisteredTypeConverter(_.toString)
  implicit val StringToString: RegisteredTypeConverter[String, String] = RegisteredTypeConverter(identity)
  // register date/time converters
  implicit val StringToDate: RegisteredTypeConverter[String, Date] = RegisteredTypeConverter(Date.valueOf)
  implicit val StringToTime: RegisteredTypeConverter[String, Time] = RegisteredTypeConverter(Time.valueOf)
  implicit val StringToTimestamp: RegisteredTypeConverter[String, Timestamp] = RegisteredTypeConverter(Timestamp.valueOf)
  implicit val DateToString: RegisteredTypeConverter[Date, String] = RegisteredTypeConverter(_.toString)
  implicit val TimeToString: RegisteredTypeConverter[Time, String] = RegisteredTypeConverter(_.toString)
  implicit val TimestampToString: RegisteredTypeConverter[Timestamp, String] = RegisteredTypeConverter(_.toString)

  implicit val StringToLocalDate: RegisteredTypeConverter[String, LocalDate] = RegisteredTypeConverter(LocalDate.parse)
  implicit val StringToLocalTime: RegisteredTypeConverter[String, LocalTime] = RegisteredTypeConverter(LocalTime.parse)

  implicit val StringToLocalDateTime: RegisteredTypeConverter[String, LocalDateTime] =
    RegisteredTypeConverter(v => LocalDateTime.parse(v.replace(' ', 'T')))

  implicit val LocalDateToString: RegisteredTypeConverter[LocalDate, String] = RegisteredTypeConverter(_.toString)
  implicit val LocalTimeToString: RegisteredTypeConverter[LocalTime, String] = RegisteredTypeConverter(_.toString)
  implicit val LocalDateTimeToString: RegisteredTypeConverter[LocalDateTime, String] = RegisteredTypeConverter(_.toString)


  def converter[FROM, TO](implicit converter: RegisteredTypeConverter[FROM, TO]): (FROM => TO) = converter.convert

  ///
  private def pgBoolAdjust(s: String): String =
    Option(s).map(_.toLowerCase) match {
      case Some("t")  => "true"
      case Some("f")  => "false"
      case _ => s
    }
}
