package com.github.tminglei.slickpg

case class Range[T](start: T, end: T, edge: EdgeType = `[_,_)`) {

  def as[A](convert: (T => A)): Range[A] = {
    new Range[A](convert(start), convert(end), edge)
  }

  override def toString = edge match {
    case `[_,_)` => s"[$start,$end)"
    case `(_,_]` => s"($start,$end]"
    case `(_,_)` => s"($start,$end)"
    case `[_,_]` => s"[$start,$end]"
  }
}

// edge type definitions
sealed trait EdgeType
case object `[_,_)` extends EdgeType
case object `(_,_]` extends EdgeType
case object `(_,_)` extends EdgeType
case object `[_,_]` extends EdgeType
