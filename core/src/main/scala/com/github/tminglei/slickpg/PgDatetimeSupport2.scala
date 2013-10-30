package com.github.tminglei.slickpg

import scala.slick.driver.{BasicProfile, PostgresDriver}
import scala.slick.lifted._
import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.ast.Node
import scala.slick.session.{PositionedResult, PositionedParameters}
import org.postgresql.util.PGobject

trait PgDatetimeSupport2 {driver: PostgresDriver =>

  type Date
  type Time
  type DateTime
  type Interval

  trait DatetimeImplicits {
//    implicit val intervalTypeMapper = new IntervalTypeMapper

    ///
    implicit def dateColumnExtensionMethods(c: Column[Date]) = new DateColumnExtensionMethods(c)
    implicit def dateOptColumnExtensionMethods(c: Column[Option[Date]]) = new DateColumnExtensionMethods(c)

    implicit def timeColumnExtensionMethods(c: Column[Time]) = new TimeColumnExtensionMethods(c)
    implicit def timeOptColumnExtensionMethods(c: Column[Option[Time]]) = new TimeColumnExtensionMethods(c)

    implicit def timestampColumnExtensionMethods(c: Column[DateTime]) = new TimestampColumnExtensionMethods(c)
    implicit def timestampOptColumnExtensionMethods(c: Column[Option[DateTime]]) = new TimestampColumnExtensionMethods(c)

    implicit def intervalColumnExtensionMethods(c: Column[Interval]) = new IntervalColumnExtensionMethods(c)
    implicit def intervalOptColumnExtensionMethods(c: Column[Option[Interval]]) = new IntervalColumnExtensionMethods(c)
  }

  ////////////////////////////////////////////////////////////////////////////////////

  object DatetimeLibrary {
    val + = new SqlOperator("+")
    val - = new SqlOperator("-")
    val * = new SqlOperator("*")
    val / = new SqlOperator("/")

    val Age = new SqlFunction("age")
    val Part = new SqlFunction("date_part")
    val Trunc = new SqlFunction("date_trunc")

    val Negative = new SqlOperator("-")
    val JustifyDays = new SqlFunction("justify_days")
    val JustifyHours = new SqlFunction("justify_hours")
    val JustifyInterval = new SqlFunction("justify_interval")
  }

  class TimestampColumnExtensionMethods[P1](val c: Column[P1])(
    implicit tm: TypeMapper[Interval]) extends ExtensionMethods[DateTime, P1] {
    def +++[P2, R](e: Column[P2])(implicit om: o#arg[Interval, P2]#to[DateTime, R]) = {
      om(DatetimeLibrary.+.column(n, Node(e)))
    }
    def - [P2, R](e: Column[P2])(implicit om: o#to[Interval, R]) = {
      om(DatetimeLibrary.-.column(n, Node(e)))
    }
    def -- [P2, R](e: Column[P2])(implicit om: o#arg[Time, P2]#to[DateTime, R]) = {
      om(DatetimeLibrary.-.column(n, Node(e)))
    }
    def ---[P2, R](e: Column[P2])(implicit om: o#arg[Interval, P2]#to[DateTime, R]) = {
      om(DatetimeLibrary.-.column(n, Node(e)))
    }

    def age[R](implicit om: o#to[Interval, R]) = om(DatetimeLibrary.Age.column(n))
    def age[P2, R](e: Column[P2])(implicit om: o#arg[DateTime, P2]#to[Interval, R]) = {
      om(DatetimeLibrary.Age.column(Node(e), n))
    }
    def part[R](field: Column[String])(implicit om: o#to[Double, R]) = {
      om(DatetimeLibrary.Part.column(Node(field), n))
    }
    def trunc[R](field: Column[String])(implicit om: o#to[DateTime, R]) = {
      om(DatetimeLibrary.Trunc.column(Node(field), n))
    }
  }

  class TimeColumnExtensionMethods[P1](val c: Column[P1])(
    implicit tm: TypeMapper[Interval]) extends ExtensionMethods[Time, P1] {
    def + [P2, R](e: Column[P2])(implicit om: o#arg[Date, P2]#to[DateTime, R]) = {
      om(DatetimeLibrary.+.column[DateTime](n, Node(e)))
    }
    def +++[P2, R](e: Column[P2])(implicit om: o#arg[Interval, P2]#to[Time, R]) = {
      om(DatetimeLibrary.+.column(n, Node(e)))
    }
    def - [P2, R](e: Column[P2])(implicit om: o#arg[Time, P2]#to[Interval, R]) = {
      om(DatetimeLibrary.-.column(n, Node(e)))
    }
    def ---[P2, R](e: Column[P2])(implicit om: o#arg[Interval, P2]#to[Time, R]) = {
      om(DatetimeLibrary.-.column(n, Node(e)))
    }
  }

  class DateColumnExtensionMethods[P1](val c: Column[P1])(
    implicit tm: TypeMapper[Interval]) extends ExtensionMethods[Date, P1] {
    def + [P2, R](e: Column[P2])(implicit om: o#arg[Time, P2]#to[DateTime, R]) = {
      om(DatetimeLibrary.+.column[DateTime](n, Node(e)))
    }
    def ++ [P2, R](e: Column[P2])(implicit om: o#arg[Int, P2]#to[Date, R]) = {
      om(DatetimeLibrary.+.column(n, Node(e)))
    }
    def +++[P2, R](e: Column[P2])(implicit om: o#arg[Interval, P2]#to[DateTime, R]) = {
      om(DatetimeLibrary.+.column[DateTime](n, Node(e)))
    }
    def - [P2, R](e: Column[P2])(implicit om: o#arg[Date, P2]#to[Int, R]) = {
      om(DatetimeLibrary.-.column(n, Node(e)))
    }
    def -- [P2, R](e: Column[P2])(implicit om: o#arg[Int, P2]#to[Date, R]) = {
      om(DatetimeLibrary.-.column(n, Node(e)))
    }
    def ---[P2, R](e: Column[P2])(implicit om: o#arg[Interval, P2]#to[DateTime, R]) = {
      om(DatetimeLibrary.-.column[DateTime](n, Node(e)))
    }
  }

  class IntervalColumnExtensionMethods[P1](val c: Column[P1])(
    implicit tm: TypeMapper[Interval]) extends ExtensionMethods[Interval, P1] {
    def + [P2, R](e: Column[P2])(implicit om: o#arg[Interval, P2]#to[Interval, R]) = {
      om(DatetimeLibrary.+.column(n, Node(e)))
    }
    def unary_-[R](implicit om: o#to[Interval, R]) = om(DatetimeLibrary.Negative.column(n))
    def - [P2, R](e: Column[P2])(implicit om: o#arg[Interval, P2]#to[Interval, R]) = {
      om(DatetimeLibrary.-.column(n, Node(e)))
    }
    def * [R](factor: Column[Double])(implicit om: o#to[Interval, R]) = {
      om(DatetimeLibrary.*.column(n, Node(factor)))
    }
    def / [R](factor: Column[Double])(implicit om: o#to[Interval, R]) = {
      om(DatetimeLibrary./.column(n, Node(factor)))
    }

    def part[R](field: Column[String])(implicit om: o#to[Double, R]) = {
      om(DatetimeLibrary.Part.column(Node(field), n))
    }
    def justifyDays[R](implicit om: o#to[Interval, R]) = om(DatetimeLibrary.JustifyDays.column(n))
    def justifyHours[R](implicit om: o#to[Interval, R]) = om(DatetimeLibrary.JustifyHours.column(n))
    def justifyInterval[R](implicit om: o#to[Interval, R]) = om(DatetimeLibrary.JustifyInterval.column(n))
  }

  ////////////////////////////////////////////////////////////////////////////////////

  class IntervalTypeMapper(fnFromString: (String => Interval), fnToString: (Interval => String) = ((r: Interval) => r.toString))
              extends TypeMapperDelegate[Interval] with BaseTypeMapper[Interval] {

    def apply(v1: BasicProfile): TypeMapperDelegate[Interval] = this

    //-----------------------------------------------------------------
    def zero: Interval = null.asInstanceOf[Interval]

    def sqlType: Int = java.sql.Types.OTHER

    def sqlTypeName: String = "interval"

    def setValue(v: Interval, p: PositionedParameters) = p.setObject(mkPgObject(v), sqlType)

    def setOption(v: Option[Interval], p: PositionedParameters) = p.setObjectOption(v.map(mkPgObject), sqlType)

    def nextValue(r: PositionedResult): Interval = r.nextStringOption().map(fnFromString).getOrElse(zero)

    def updateValue(v: Interval, r: PositionedResult) = r.updateObject(mkPgObject(v))

    override def valueToSQLLiteral(v: Interval) = fnToString(v)

    ///
    private def mkPgObject(v: Interval) = {
      val obj = new PGobject
      obj.setType(sqlTypeName)
      obj.setValue(valueToSQLLiteral(v))
      obj
    }
  }

}
