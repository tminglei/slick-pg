package com.github.tminglei.slickpg.agg

import slick.ast._
import slick.jdbc.JdbcType
import slick.lifted.{OptionMapperDSL, Ordered, Rep}
import slick.util.ConstArray

/**
  * pg aggregate function support, usage:
  * {{{
  *  object AggregateLibrary {
  *    val StringAgg = new SqlFunction("string_agg")
  *  }
  *  case class StringAgg(delimiter: String) extends UnaryAggFuncPartsBasic[String, String](AggregateLibrary.StringAgg, List(LiteralNode(delimiter)))
  *  ...
  *  col1 ^: StringAgg(",").forDistinct().orderBy(col1 desc) <=> string_agg(distinct col1, ',', order by col1 desc)
  * }}}
  */
trait PgAggFuncBase {

  final case class AggFuncInputs(aggParams: ConstArray[Node],
                                 modifier: Option[String] = None,
                                 orderBy: ConstArray[(Node, Ordering)] = ConstArray.empty
                                 ) extends SimplyTypedNode {
    type Self = AggFuncInputs
    override protected def buildType = aggParams(0).nodeType
    override def children: ConstArray[Node] = aggParams ++ orderBy.map(_._1)
    override protected[this] def rebuild(ch: ConstArray[Node]): AggFuncInputs = {
      val newAggParams = ch.slice(0, aggParams.length)
      val orderByOffset = aggParams.length
      val newOrderBy = ch.slice(orderByOffset, orderByOffset + orderBy.length)
      copy(aggParams = newAggParams,
        orderBy = (orderBy).zip(newOrderBy).map { case ((_, o), n) => (n, o) })
    }
  }

  ///--- used to hold aggregate function's parts
  trait AggFuncParts {
    def aggFunc: FunctionSymbol
    def params: ConstArray[Node]
    def modifier: Option[String]
    def ordered: Option[Ordered]
  }
  protected sealed class AggFuncPartsImpl(
    val aggFunc: FunctionSymbol,
    val params: ConstArray[Node] = ConstArray.empty,
    val modifier: Option[String] = None,
    val ordered: Option[Ordered] = None
  ) extends AggFuncParts

  ///--- used to support one column member's functions
  trait UnaryAggFunction[T,R] { parts: AggFuncParts =>
    def ^:[P1,PR](expr: Rep[P1])(implicit tm: JdbcType[R], om: OptionMapperDSL.arg[T,P1]#to[R,PR]): Rep[PR] = {
      val aggParams = expr.toNode +: params
      val orderBy = ConstArray.from(ordered).flatMap(o => ConstArray.from(o.columns))
      om.column(aggFunc, AggFuncInputs(aggParams, modifier, orderBy))
    }
  }
  class UnaryAggFuncPartsBasic[T,R](aggFunc: FunctionSymbol, params: Seq[Node] = Nil)
          extends AggFuncPartsImpl(aggFunc, ConstArray.from(params)) with UnaryAggFunction[T,R] {
    def forDistinct() = new UnaryAggFuncPartsWithModifier[T,R](aggFunc, params, Some("distinct"))
    def orderBy(ordered: Ordered) = new AggFuncPartsImpl(aggFunc, ConstArray.from(params), None, Some(ordered)) with UnaryAggFunction[T,R]
  }
  class UnaryAggFuncPartsWithModifier[T,R](aggFunc: FunctionSymbol, params: Seq[Node] = Nil, modifier: Option[String] = None)
          extends AggFuncPartsImpl(aggFunc, ConstArray.from(params), modifier) with UnaryAggFunction[T,R] {
    def orderBy(ordered: Ordered) = new AggFuncPartsImpl(aggFunc, ConstArray.from(params), modifier, Some(ordered)) with UnaryAggFunction[T,R]
  }

  ///--- used to support two column members' functions
  trait BinaryAggFunction[T,R] { parts: AggFuncParts =>
    def ^:[P1,P2,PR](expr: (Rep[P1], Rep[P2]))(implicit tm: JdbcType[R], om: OptionMapperDSL.arg[T,P1]#arg[T,P2]#to[R,PR]): Rep[PR] = {
      val aggParams = expr._1.toNode +: expr._2.toNode +: params
      val orderBy = ConstArray.from(ordered).flatMap(o => ConstArray.from(o.columns))
      om.column(aggFunc, AggFuncInputs(aggParams, modifier, orderBy))
    }
  }
  class BinaryAggFuncPartsBasic[T,R](aggFunc: FunctionSymbol, params: Seq[Node] = Nil)
          extends AggFuncPartsImpl(aggFunc, ConstArray.from(params)) with BinaryAggFunction[T,R] {
    def forDistinct() = new BinaryAggFuncPartsWithModifier[T,R](aggFunc, params, Some("distinct"))
    def orderBy(ordered: Ordered) = new AggFuncPartsImpl(aggFunc, ConstArray.from(params), None, Some(ordered)) with BinaryAggFunction[T,R]
  }
  class BinaryAggFuncPartsWithModifier[T,R](aggFunc: FunctionSymbol, params: Seq[Node] = Nil, modifier: Option[String] = None)
          extends AggFuncPartsImpl(aggFunc, ConstArray.from(params), modifier) with BinaryAggFunction[T,R] {
    def orderBy(ordered: Ordered) = new AggFuncPartsImpl(aggFunc, ConstArray.from(params), modifier, Some(ordered)) with BinaryAggFunction[T,R]
  }

  ///--- used to support three column members' functions
  trait TernaryAggFunction[T,R] { parts: AggFuncParts =>
    def ^:[P1,P2,P3,PR](expr: (Rep[P1], Rep[P2], Rep[P3]))(implicit tm: JdbcType[R], om: OptionMapperDSL.arg[T,P1]#arg[T,P2]#arg[T,P3]#to[R,PR]): Rep[PR] = {
      val aggParams = expr._1.toNode +: expr._2.toNode +: expr._3.toNode +: params
      val orderBy = ConstArray.from(ordered).flatMap(o => ConstArray.from(o.columns))
      om.column(aggFunc, AggFuncInputs(aggParams, modifier, orderBy))
    }
  }
  class TernaryAggFuncPartsBasic[T,R](aggFunc: FunctionSymbol, params: Seq[Node] = Nil)
          extends AggFuncPartsImpl(aggFunc, ConstArray.from(params)) with TernaryAggFunction[T,R] {
    def forDistinct() = new TernaryAggFuncPartsWithModifier[T,R](aggFunc, params, Some("distinct"))
    def orderBy(ordered: Ordered) = new AggFuncPartsImpl(aggFunc, ConstArray.from(params), None, Some(ordered)) with TernaryAggFunction[T,R]
  }
  class TernaryAggFuncPartsWithModifier[T,R](aggFunc: FunctionSymbol, params: Seq[Node] = Nil, modifier: Option[String] = None)
          extends AggFuncPartsImpl(aggFunc, ConstArray.from(params), modifier) with TernaryAggFunction[T,R] {
    def orderBy(ordered: Ordered) = new AggFuncPartsImpl(aggFunc, ConstArray.from(params), modifier, Some(ordered)) with TernaryAggFunction[T,R]
  }
}
