package com.github.tminglei.slickpg

import java.util.Date

case class Range[T](start: T, end: T, edges: Range.Edges = Range.CloseOpen) {

  def as[A](convert: (T => A)): Range[A] = {
    new Range[A](convert(start), convert(end), edges)
  }

  override def toString = edges match {
    case Range.CloseOpen  => s"[$start,$end)"
    case Range.OpenClose  => s"($start,$end]"
    case Range.OpenOpen   => s"($start,$end)"
    case Range.CloseClose => s"[$start,$end]"
  }
}

object Range {
  /* range edge types */
  sealed trait Edges
  case object CloseOpen   extends Edges       // matches: '[start,end)'
  case object OpenClose   extends Edges       // matches: '(start,end]'
  case object OpenOpen    extends Edges       // matches: '(start,end)'
  case object CloseClose  extends Edges       // matches: '[start,end]'

  // regular expr matchers to range string
  val CloseOpenRange  = """\["?([^,]*)"?,[ ]*"?([^,]*)"?\)""".r   // matches: [start,end)
  val OpenCloseRange  = """\("?([^,]*)"?,[ ]*"?([^,]*)"?\]""".r   // matches: (start,end]
  val OpenOpenRange   = """\("?([^,]*)"?,[ ]*"?([^,]*)"?\)""".r   // matches: (start,end)
  val CloseCloseRange = """\["?([^,]*)"?,[ ]*"?([^,]*)"?\]""".r   // matches: [start,end]

  def mkParser[T](convert: (String => T)): (String => Range[T]) =
    (str: String) => str match {
      case CloseOpenRange(start, end)   => Range(convert(start), convert(end), CloseOpen)
      case OpenCloseRange(start, end)   => Range(convert(start), convert(end), OpenClose)
      case OpenOpenRange (start, end)   => Range(convert(start), convert(end), OpenOpen)
      case CloseCloseRange(start, end)  => Range(convert(start), convert(end), CloseClose)
    }

  ///
  def mkWithLength[T](start: T, length: Double, edge: Edges = CloseOpen) = {
    val upper = (start.asInstanceOf[Double] + length).asInstanceOf[T]
    new Range[T](start, upper, edge)
  }

  def mkWithInterval[T <: Date](start: T, interval: Interval, edge: Edges = CloseOpen) = {
    val end = (start +: interval).asInstanceOf[T]
    new Range[T](start, end, edge)
  }
}
