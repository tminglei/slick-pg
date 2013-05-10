package org.slick

import Range._
case class Range[T](lower: T, upper: T, edge: Edge = IncInf) {
  override def toString = edge match {
    case IncInf => s"[$lower,$upper)"
    case InfInc => s"($lower,$upper]"
    case InfInf => s"($lower,$upper)"
    case IncInc => s"[$lower,$upper]"
  }
}

object Range {
  sealed trait Edge
  case object IncInf extends Edge  /* inclusive|infinite: '[lower,upper)' */
  case object InfInc extends Edge  /* infinite|inclusive: '(lower,upper]' */
  case object InfInf extends Edge  /* infinite|infinite:  '(lower,upper)' */
  case object IncInc extends Edge  /* inclusive|inclusive: '[lower,upper]' */
}
