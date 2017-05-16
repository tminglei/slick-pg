package com.github.tminglei.slickpg.date

import java.sql.{PreparedStatement, ResultSet, Timestamp}
import java.time.format.DateTimeFormatter
import java.time._
import java.util.Calendar

import org.postgresql.util.{PGInterval}
import slick.ast.FieldSymbol
import slick.jdbc.{JdbcType, JdbcTypesComponent, PostgresProfile}

import scala.reflect.ClassTag

/**
  * Created by minglei on 5/15/17.
  */
trait PgDateJdbcTypes extends JdbcTypesComponent { driver: PostgresProfile =>

  class GenericDateJdbcType[T: ClassTag](val sqlTypeName: String, val sqlType: Int,
                                         override val hasLiteralForm: Boolean = false
                                        ) extends DriverJdbcType[T] {

    classTag.runtimeClass match {
      case clazz if clazz == classOf[LocalDate] =>
      case clazz if clazz == classOf[LocalTime] =>
      case clazz if clazz == classOf[LocalDateTime] =>
      case clazz if clazz == classOf[OffsetDateTime] =>
      case clazz if clazz == classOf[Instant]   =>
      case clazz if clazz == classOf[Duration]  =>
      case clazz if clazz == classOf[Calendar]  =>
      case clazz  => throw new IllegalArgumentException("Unsupported type: " + clazz.getName)
    }

    ///---

    override def sqlTypeName(sym: Option[FieldSymbol]): String = sqlTypeName

    override def getValue(r: ResultSet, idx: Int): T = {
      val value = classTag.runtimeClass match {
        case clazz if clazz == classOf[Instant]  => Option(r.getTimestamp(idx)).map(_.toInstant).orNull
        case clazz if clazz == classOf[Duration] => Option(r.getObject(idx, classOf[PGInterval])).map(pgInterval2Duration).orNull
        case clazz                               => r.getObject(idx, clazz)
      }
      if (r.wasNull) null.asInstanceOf[T] else value.asInstanceOf[T]
    }

    override def setValue(v: T, p: PreparedStatement, idx: Int): Unit =
      if (v == null) p.setNull(idx, sqlType) else classTag.runtimeClass match {
        case clazz if clazz == classOf[Calendar] => p.setObject(idx, v.asInstanceOf[Calendar].toInstant.atOffset(ZoneOffset.UTC), sqlType)
        case clazz if clazz == classOf[Instant]  => p.setObject(idx, instantToTimestamp(v.asInstanceOf[Instant]), sqlType)
        case clazz                               => p.setObject(idx, v, sqlType)
      }

    override def updateValue(v: T, r: ResultSet, idx: Int): Unit =
      if (v == null) r.updateNull(idx) else classTag.runtimeClass match {
        case clazz if clazz == classOf[Calendar] => r.updateObject(idx, v.asInstanceOf[Calendar].toInstant.atOffset(ZoneOffset.UTC))
        case clazz if clazz == classOf[Instant]  => r.updateObject(idx, instantToTimestamp(v.asInstanceOf[Instant]))
        case clazz                               => r.updateObject(idx, v)
      }

    override def valueToSQLLiteral(v: T) = if (v == null) "NULL" else classTag.runtimeClass match {
      case clazz if clazz == classOf[Calendar] =>
        s"'${v.asInstanceOf[Calendar].toInstant.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}'"
      case clazz      =>    s"'$v'"
    }

    ///---

    def mapTo[U](tmap: T => U, tcomap: U => T)(implicit ctag: ClassTag[U]): JdbcType[U] =
      MappedJdbcType.base(tcomap, tmap)(ctag, this)

    /// pg interval string --> time.Duration
    private def pgInterval2Duration(pgInterval: PGInterval): Duration = {
      Duration.ofDays(pgInterval.getYears * 365 + pgInterval.getMonths * 30 + pgInterval.getDays)
        .plusHours(pgInterval.getHours)
        .plusMinutes(pgInterval.getMinutes)
        .plusNanos(Math.round(pgInterval.getSeconds * 1000 * 1000000))
    }

    private def instantToTimestamp(v: Instant) = v match {
      case Instant.MAX  => "infinity"
      case Instant.MIN  => "-infinity"
      case finite   => Timestamp.from(finite)
    }
  }
}
