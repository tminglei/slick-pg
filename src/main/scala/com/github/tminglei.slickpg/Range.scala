package com.github.tminglei.slickpg

case class Range[T](lower: T, upper: T, edge: Range.Edge = Range.IncInf) {

  def as[A](convert: (T => A)): Range[A] = {
    new Range[A](convert(lower), convert(upper), edge)
  }

  override def toString = edge match {
    case Range.IncInf => s"[$lower,$upper)"
    case Range.InfInc => s"($lower,$upper]"
    case Range.InfInf => s"($lower,$upper)"
    case Range.IncInc => s"[$lower,$upper]"
  }
}

object Range {
  /* range edge types */
  sealed trait Edge
  case object IncInf extends Edge     // inclusive + infinite:  '[lower,upper)'
  case object InfInc extends Edge     // infinite  + inclusive: '(lower,upper]'
  case object InfInf extends Edge     // infinite  + infinite:  '(lower,upper)'
  case object IncInc extends Edge     // inclusive + inclusive: '[lower,upper]'

  // regular expr matchers to range string
  val IncInfRange = """\["?([^,]*)"?,[ ]*"?([^,]*)"?\)""".r   //matches: [lower,upper)
  val InfIncRange = """\("?([^,]*)"?,[ ]*"?([^,]*)"?\]""".r   //matches: (lower,upper]
  val InfInfRange = """\("?([^,]*)"?,[ ]*"?([^,]*)"?\)""".r   //matches: (lower,upper)
  val IncIncRange = """\["?([^,]*)"?,[ ]*"?([^,]*)"?\]""".r   //matches: [lower,upper]

  def mkParser[T](convert: (String => T)): (String => Range[T]) =
    (str: String) => str match {
      case IncInfRange(lower, upper) => Range(convert(lower), convert(upper), IncInf)
      case InfIncRange(lower, upper) => Range(convert(lower), convert(upper), InfInc)
      case InfInfRange(lower, upper) => Range(convert(lower), convert(upper), InfInf)
      case IncIncRange(lower, upper) => Range(convert(lower), convert(upper), IncInc)
    }
}
