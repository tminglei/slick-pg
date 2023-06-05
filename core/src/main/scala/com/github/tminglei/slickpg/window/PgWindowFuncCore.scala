package com.github.tminglei.slickpg
package window

import com.github.tminglei.slickpg.agg.AggFuncRep
import slick.ast._
import slick.lifted.Rep.TypedRep
import slick.lifted.{CanBeQueryCondition, Ordered, Rep}
import slick.util.ConstArray

/** A window function call expression */
final case class WindowFuncExpr(
  aggFuncExpr: Node,
  partitionBy: ConstArray[Node],
  orderBy: ConstArray[(Node, Ordering)],
  frameDef: Option[(String, String, Option[String])] = None
) extends SimplyTypedNode {
  type Self = WindowFuncExpr
  protected def buildType = aggFuncExpr.nodeType
  override def children: ConstArray[Node] = aggFuncExpr +: (partitionBy ++ orderBy.map(_._1))
  override protected[this] def rebuild(ch: ConstArray[Node]): Self = {
    val newAggFuncExpr = ch(0)
    val partitionByOffset = 1
    val newPartitionBy = ch.slice(partitionByOffset, partitionByOffset + partitionBy.length)
    val orderByOffset = partitionByOffset + partitionBy.length
    val newOrderBy = ch.slice(orderByOffset, orderByOffset + orderBy.length)
    copy(aggFuncExpr = newAggFuncExpr, partitionBy = newPartitionBy,
      orderBy = orderBy.zip(newOrderBy).map { case ((_, o), n) => (n, o) }
    )
  }
}

/**
  * pg window function support, usage:
  * {{{
  *  object AggregateLibrary {
  *    val Avg = new SqlFunction("avg")
  *  }
  *  def avg[T : JdbcType](c: Rep[T]) = agg.AggFuncRep[T](AggLibrary.Avg, List(c.toNode))
  *  ...
  *  avg(salary).over.partitionBy(dept).orderBy(dept,salary)
  *                   .rowsFrame(RowCursor.UnboundPreceding, RowCursor.CurrentRow)
  *    <=> avg(salary) over (partition by dept order by dept, salary rows between unbounded preceding and current row)
  * }}}
  */

object WindowFunc {
  def apply[R: TypedType](aggFunc: FunctionSymbol, params: Seq[Node]): WindowFunc[R] =
    WindowFunc[R](agg.AggFuncParts(aggFunc, params))

  implicit def winFunc2aggFuncRep[R: TypedType](winFunc: WindowFunc[R]): AggFuncRep[R] =
    new agg.AggFuncRep[R](winFunc._parts)
}
case class WindowFunc[R: TypedType](_parts: agg.AggFuncParts) {
  def filter[F <: Rep[_]](where: => F)(implicit wt: CanBeQueryCondition[F]) =
    copy(_parts.copy(filter = Some(wt(where).toNode)))
  def over = WindowFuncRep[R](_parts.toNode(implicitly[TypedType[R]]))
}

case class WindowFuncRep[R: TypedType](
  _aggFuncExpr: Node,
  _partitionBy: ConstArray[Node] = ConstArray.empty,
  _orderBy: ConstArray[(Node, Ordering)] = ConstArray.empty,
  _frameDef: Option[(String, String, Option[String])] = None
) extends TypedRep[R] {
  def partitionBy(columns: Rep[_]*) = copy(_partitionBy = ConstArray.from(columns.map(_.toNode)))
  def sortBy(ordered: Ordered) = copy(_orderBy = ConstArray.from(ordered.columns))
  def rowsFrame(start: RowCursor, end: Option[RowCursor] = None) =
    copy(_frameDef = Some((FrameMode.ROWS_MODE, start.desc, end.map(_.desc))))
  def rangeFrame(start: RowCursor, end: Option[RowCursor] = None) =
    copy(_frameDef = Some((FrameMode.RANGE_MODE, start.desc, end.map(_.desc))))
  def toNode: Node = WindowFuncExpr(_aggFuncExpr, _partitionBy, _orderBy, _frameDef)
}

///---

sealed class RowCursor(val desc: String)

object RowCursor {
  case object CurrentRow extends RowCursor("current row")
  case class BoundPreceding[T <: AnyVal](value: T) extends RowCursor(s"$value preceding")
  case object UnboundPreceding extends RowCursor("unbounded preceding")
  case class BoundFollowing[T <: AnyVal](value: T) extends RowCursor(s"$value following")
  case object UnboundFollowing extends RowCursor("unbounded following")
}

object FrameMode {
  val ROWS_MODE  = "rows"
  val RANGE_MODE = "range"
}

case class Over(
  _partitionBy: ConstArray[Node] = ConstArray.empty,
  _orderBy: ConstArray[(Node, Ordering)] = ConstArray.empty,
  _frameDef: Option[(String, String, Option[String])] = None
) {
  def partitionBy(columns: Rep[_]*) = copy(_partitionBy = ConstArray.from(columns.map(_.toNode)))
  def sortBy(ordered: Ordered) = copy(_orderBy = ConstArray.from(ordered.columns))
  def rowsFrame(start: RowCursor, end: Option[RowCursor] = None) =
    copy(_frameDef = Some((FrameMode.ROWS_MODE, start.desc, end.map(_.desc))))
  def rangeFrame(start: RowCursor, end: Option[RowCursor] = None) =
    copy(_frameDef = Some((FrameMode.RANGE_MODE, start.desc, end.map(_.desc))))
  def ::[R: TypedType](aggFunc: agg.AggFuncRep[R]) =
    WindowFuncRep[R](aggFunc.toNode, _partitionBy, _orderBy, _frameDef)
}
