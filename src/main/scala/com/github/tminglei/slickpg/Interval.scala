package com.github.tminglei.slickpg

import java.text.DecimalFormat
import java.util.{Date, Calendar}
import org.postgresql.util.PGInterval

/**
 * copy from [[org.postgresql.util.PGInterval]],
 * should be more convenient to be used in scala environment
 */
case class Interval(
            years: Int,
            months: Int,
            days: Int,
            hours: Int,
            minutes: Int,
            seconds: Double) {

  def milliseconds: Int = (microseconds + (if (microseconds < 0) -500 else 500)) / 1000
  def microseconds: Int = (seconds * 1000000.0).asInstanceOf[Int]

  def +:(cal: Calendar): Calendar = {
    cal.add(Calendar.MILLISECOND, milliseconds)
    cal.add(Calendar.MINUTE, minutes)
    cal.add(Calendar.HOUR, hours)
    cal.add(Calendar.DAY_OF_MONTH, days)
    cal.add(Calendar.MONTH, months)
    cal.add(Calendar.YEAR, years)
    cal
  }

  def +:(date: Date): Date = {
    val cal = Calendar.getInstance
    cal.setTime(date)
    date.setTime((cal +: this).getTime.getTime)
    date
  }

  def +(other: Interval): Interval = {
    new Interval(
      years + other.years,
      months + other.months,
      days + other.days,
      hours + other.hours,
      minutes + other.minutes,
      seconds + other.seconds
    )
  }

  def *(factor: Int): Interval = {
    new Interval(
      years * factor,
      months * factor,
      days * factor,
      hours * factor,
      minutes * factor,
      seconds * factor
    )
  }

  override def toString = {
    val secs = Interval.secondsFormat.format(seconds)
    s"$years years $months mons $days days $hours hours $minutes mins $secs secs"
  }
}

object Interval {
  private val secondsFormat = new DecimalFormat("0.00####")

  def apply(interval: String): Interval = fromPgInterval(new PGInterval(interval))

  def fromPgInterval(interval: PGInterval): Interval = {
    new Interval(
      interval.getYears,
      interval.getMonths,
      interval.getDays,
      interval.getHours,
      interval.getMinutes,
      interval.getSeconds
    )
  }

  def toPgInterval(interval: Interval): PGInterval = {
    new PGInterval(
      interval.years,
      interval.months,
      interval.days,
      interval.hours,
      interval.minutes,
      interval.seconds
    )
  }
}