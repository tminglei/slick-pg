package com.github.tminglei.slickpg.agg

import slick.ast._
import slick.jdbc.JdbcType
import slick.lifted._
import slick.util.ConstArray

/** An aggregate function call expression */
final case class AggFuncExpr(
  aggFunc: FunctionSymbol,
  params: ConstArray[Node],
  orderBy: ConstArray[(Node, Ordering)] = ConstArray.empty,
  filter: Option[Node] = None,
  distinct: Boolean = false,
  forOrdered: Boolean = false
)(val buildType: Type) extends SimplyTypedNode {
  type Self = AggFuncExpr
  override def children: ConstArray[Node] = params ++ orderBy.map(_._1) ++ ConstArray.from(filter)
  override protected[this] def rebuild(ch: ConstArray[Node]): AggFuncExpr = {
    val newAggParams = ch.slice(0, params.length)
    val orderByOffset = params.length
    val newOrderBy = ch.slice(orderByOffset, orderByOffset + orderBy.length)
    val filterOffset = orderByOffset + orderBy.length
    val filter = if (ch.length > filterOffset) Some(ch.last) else None
    copy(params = newAggParams, filter = filter,
      orderBy = (orderBy).zip(newOrderBy).map { case ((_, o), n) => (n, o) }
    )(buildType)
  }
}

/**
  * pg aggregate function support, usage:
  * {{{
  *  object AggregateLibrary {
  *    val StringAgg = new SqlFunction("string_agg")
  *  }
  *  case class StringAgg(delimiter: String) extends UnaryAggFuncPartsBasic[String, String](AggregateLibrary.StringAgg, List(LiteralNode(delimiter)))
  *  ...
  *  col1 ^: StringAgg(",").distinct().sortBy(col1 desc) <=> string_agg(distinct col1, ',', order by col1 desc)
  * }}}
  */
trait PgAggFuncBase {

  trait AggFuncParts {
    def aggFunc: FunctionSymbol
    def params: Seq[Node]
    def ordered: Option[Ordered]
    def where : Option[Node]
    def distinct_ : Boolean
    def forOrderedSet: Boolean
  }
  protected sealed class AggFuncPartsImpl(
    val aggFunc: FunctionSymbol,
    val params: Seq[Node] = Nil,
    val ordered: Option[Ordered] = None,
    val where : Option[Node] = None,
    val distinct_ : Boolean = false,
    val forOrderedSet: Boolean = false
  ) extends AggFuncParts

  ///--- for normal data set
  class AggFuncPartsBase[T,R](aggFunc: FunctionSymbol, params: Seq[Node] = Nil)
          extends AggFuncPartsImpl(aggFunc, params) with ToAggFunction[T,R] {
    def distinct() = new AggFuncPartsWithModifier[T,R](aggFunc, params, distinct = true)
    def sortBy(ordered: Ordered) = new AggFuncPartsWithModOrderBy[T,R](aggFunc, params, Some(ordered), distinct = false)
    def filter[W <: Rep[_]](where: => W)(implicit wt: CanBeQueryCondition[W]) =
      new AggFuncPartsImpl(aggFunc, params, ordered, Some(wt(where).toNode), distinct_ = false) with ToAggFunction[T,R]
  }
  private[agg] class AggFuncPartsWithModifier[T,R](aggFunc: FunctionSymbol, params: Seq[Node], distinct: Boolean)
          extends AggFuncPartsImpl(aggFunc, params, distinct_ = distinct) with ToAggFunction[T,R] {
    def sortBy(ordered: Ordered) = new AggFuncPartsWithModOrderBy[T,R](aggFunc, params, Some(ordered), distinct = distinct)
    def filter[W <: Rep[_]](where: => W)(implicit wt: CanBeQueryCondition[W]) =
      new AggFuncPartsImpl(aggFunc, params, ordered, Some(wt(where).toNode), distinct_ = distinct) with ToAggFunction[T,R]
  }
  private[agg] class AggFuncPartsWithModOrderBy[T,R](aggFunc: FunctionSymbol, params: Seq[Node], ordered: Option[Ordered], distinct: Boolean)
          extends AggFuncPartsImpl(aggFunc, params, distinct_ = distinct) with ToAggFunction[T,R] {
    def filter[W <: Rep[_]](where: => W)(implicit wt: CanBeQueryCondition[W]) =
      new AggFuncPartsImpl(aggFunc, params, ordered, Some(wt(where).toNode), distinct_ = distinct) with ToAggFunction[T,R]
  }

  private[agg] trait ToAggFunction[T,R] { parts: AggFuncParts =>
    def ^:[P1,PR](expr: Rep[P1])(implicit tm: JdbcType[R], om: OptionMapperDSL.arg[T,P1]#to[R,PR]): Rep[PR] = {
      mkRep(expr.toNode +: params)(om.liftedType)
    }
    def ^:[P1,P2,PR](expr: (Rep[P1], Rep[P2]))(implicit tm: JdbcType[R], om: OptionMapperDSL.arg[T,P1]#arg[T,P2]#to[R,PR]): Rep[PR] = {
      mkRep(expr._1.toNode +: expr._2.toNode +: params)(om.liftedType)
    }
    def ^:[P1,P2,P3,PR](expr: (Rep[P1], Rep[P2], Rep[P3]))(implicit tm: JdbcType[R], om: OptionMapperDSL.arg[T,P1]#arg[T,P2]#arg[T,P3]#to[R,PR]): Rep[PR] = {
      mkRep(expr._1.toNode +: expr._2.toNode +: expr._3.toNode +: params)(om.liftedType)
    }

    private def mkRep[PR](params: Seq[Node])(implicit tt: TypedType[PR]) = {
      import slick.ast.Util.nodeToNodeOps
      val params1 = ConstArray.from(params)
      val orderBy = ConstArray.from(ordered).flatMap(o => ConstArray.from(o.columns))
      Rep.forNode[PR](AggFuncExpr(aggFunc, params1, orderBy, where, distinct = distinct_)(tt).replace({
        case n @ LiteralNode(v) => n.infer()
      }))
    }
  }

  ///--- for ordered data set
}
