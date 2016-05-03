package com.github.tminglei.slickpg.agg

import slick.ast._
import slick.jdbc.JdbcType
import slick.lifted._
import slick.util.ConstArray
import slick.ast.Util.nodeToNodeOps

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
  protected class AggFuncPartsImpl(
    val aggFunc: FunctionSymbol,
    val params: Seq[Node] = Nil,
    val ordered: Option[Ordered] = None,
    val where : Option[Node] = None,
    val distinct_ : Boolean = false,
    val forOrderedSet: Boolean = false
  ) extends AggFuncParts

  ///--- for normal data set
  class AggFuncPartsBase[P,R](aggFunc: FunctionSymbol, params: Seq[Node] = Nil)
          extends AggFuncPartsWithDistinct[P,R](aggFunc, params) {
    def distinct() = new AggFuncPartsWithDistinct[P,R](aggFunc, params, distinct = true)
  }
  private[agg] class AggFuncPartsWithDistinct[P,R](aggFunc: FunctionSymbol, params: Seq[Node], distinct: Boolean = false)
          extends AggFuncPartsWithDistinctOrderBy[P,R](aggFunc, params, distinct = distinct) {
    def sortBy(ordered: Ordered) = new AggFuncPartsWithDistinctOrderBy[P,R](aggFunc, params, ordered = Some(ordered), distinct = distinct)
  }
  private[agg] class AggFuncPartsWithDistinctOrderBy[P,R](aggFunc: FunctionSymbol, params: Seq[Node], distinct: Boolean, ordered: Option[Ordered] = None)
          extends AggFuncPartsImpl(aggFunc, params, distinct_ = distinct) with ToAggFunction[P,R] {
    def filter[W <: Rep[_]](where: => W)(implicit wt: CanBeQueryCondition[W]) =
      new AggFuncPartsImpl(aggFunc, params, ordered, Some(wt(where).toNode), distinct_ = distinct) with ToAggFunction[P,R]
  }

  private[agg] trait ToAggFunction[P,R] { parts: AggFuncParts =>
    def ^:[P1,PR](expr: Rep[P1])(implicit tm: JdbcType[R], om: OptionMapperDSL.arg[P,P1]#to[R,PR]): Rep[PR] = {
      mkRep(expr.toNode +: params)(om.liftedType)
    }
    def ^:[P1,P2,PR](expr: (Rep[P1], Rep[P2]))(implicit tm: JdbcType[R], om: OptionMapperDSL.arg[P,P1]#arg[P,P2]#to[R,PR]): Rep[PR] = {
      mkRep(expr._1.toNode +: expr._2.toNode +: params)(om.liftedType)
    }
    def ^:[P1,P2,P3,PR](expr: (Rep[P1], Rep[P2], Rep[P3]))(implicit tm: JdbcType[R], om: OptionMapperDSL.arg[P,P1]#arg[P,P2]#arg[P,P3]#to[R,PR]): Rep[PR] = {
      mkRep(expr._1.toNode +: expr._2.toNode +: expr._3.toNode +: params)(om.liftedType)
    }

    private def mkRep[PR](params: Seq[Node])(implicit tt: TypedType[PR]) = {
      val params1 = ConstArray.from(params)
      val orderBy = ConstArray.from(ordered).flatMap(o => ConstArray.from(o.columns))
      Rep.forNode[PR](AggFuncExpr(aggFunc, params1, orderBy, where, distinct = distinct_)(tt).replace({
        case n @ LiteralNode(v) => n.infer()
      }))
    }
  }

  ///--- for ordered data set
  class OrderedAggFuncPartsBase[P,R](aggFunc: FunctionSymbol, params: Seq[Node] = Nil) extends OrderedAggFuncPartsWithFilter[P,R](aggFunc, params) {
    def filter[W <: Rep[_]](where: => W)(implicit wt: CanBeQueryCondition[W]) =
      new OrderedAggFuncPartsWithFilter[P,R](aggFunc, params, where = Some(wt(where).toNode))
  }
  private[agg] class OrderedAggFuncPartsWithFilter[P,R](aggFunc: FunctionSymbol, params: Seq[Node], where: Option[Node] = None)
          extends AggFuncPartsImpl(aggFunc, params, where = where) {
    def within[P1,PR](ordered: ColumnOrdered[P1])(implicit tm: JdbcType[R], om: OptionMapperDSL.arg[P,P1]#to[R,PR]): Rep[PR] = {
      implicit val tt = om.liftedType
      val params1 = ConstArray.from(params)
      val orderBy = ConstArray.from(ordered.columns)
      Rep.forNode[PR](AggFuncExpr(aggFunc, params1, orderBy, where, distinct = distinct_, forOrdered = true)(tt).replace({
        case n @ LiteralNode(v) => n.infer()
      }))
    }
  }
}
