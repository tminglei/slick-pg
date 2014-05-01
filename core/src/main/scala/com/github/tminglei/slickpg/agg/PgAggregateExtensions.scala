package com.github.tminglei.slickpg
package agg

import scala.slick.ast.Library.SqlFunction
import scala.slick.driver.PostgresDriver
import scala.slick.ast.LiteralNode

trait PgAggregateExtensions { driver: PostgresDriver =>
  import driver.simple._

  object AggregateLibrary {
    // General-Purpose Aggregate Functions
    val Average  = new SqlFunction("avg")
    val Maximum  = new SqlFunction("max")
    val Minimum  = new SqlFunction("min")
    val Sum      = new SqlFunction("sum")
    val Count    = new SqlFunction("count")

    val BitAnd   = new SqlFunction("bit_and")
    val BitOr    = new SqlFunction("bit_or")
    val BoolAnd  = new SqlFunction("bool_and")
    val BoolOr   = new SqlFunction("bool_or")
    val Every    = new SqlFunction("every")

    val ArrayAgg = new SqlFunction("array_agg")
    val StringAgg = new SqlFunction("string_agg")
//    val JsonAgg  = new SqlFunction("json_agg")  // can't be implemented yet
//    val XmlAgg   = new SqlFunction("xmlagg")    // can't be implemented yet

    // Aggregate Functions for Statistics
    val Corr  = new SqlFunction("corr")
    val CovarPop = new SqlFunction("covar_pop")
    val CovarSamp = new SqlFunction("covar_samp")
    val RegrAvgX = new SqlFunction("regr_avgx")
    val RegrAvgY = new SqlFunction("regr_avgy")
    val RegrCount = new SqlFunction("regr_count")
    val RegrIntercept = new SqlFunction("regr_intercept")
    val RegrR2 = new SqlFunction("regr_r2")
    val RegrSlope = new SqlFunction("regr_slope")
    val RegrSXX = new SqlFunction("regr_sxx")
    val RegrSXY = new SqlFunction("regr_sxy")
    val RegrSYY = new SqlFunction("regr_syy")

    val StdDev  = new SqlFunction("stddev")
    val StdDevPop = new SqlFunction("stddev_pop")
    val StdDevSamp = new SqlFunction("stddev_samp")
    val Variance = new SqlFunction("variance")
    val VarPop  = new SqlFunction("var_pop")
    val VarSamp = new SqlFunction("var_samp")
  }

  trait PgAggregateFunctions {
    // General-Purpose Aggregate Functions
    case class Avg[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.Average)
    case class Max[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.Maximum)
    case class Min[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.Minimum)
    case class Sum[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.Sum)
    case class Count[T]()  extends UnaryAggFuncPartsBasic[T, Long](AggregateLibrary.Count)

    case class BitAnd[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.BitAnd)
    case class BitOr[T]()  extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.BitOr)
    case class BoolAnd() extends UnaryAggFuncPartsBasic[Boolean, Boolean](AggregateLibrary.BoolAnd)
    case class BoolOr()  extends UnaryAggFuncPartsBasic[Boolean, Boolean](AggregateLibrary.BoolOr)
    case class Every() extends UnaryAggFuncPartsBasic[Boolean, Boolean](AggregateLibrary.Every)

    case class ArrayAgg[T]() extends UnaryAggFuncPartsBasic[T, List[T]](AggregateLibrary.ArrayAgg)
    case class StringAgg(delimiter: String) extends UnaryAggFuncPartsBasic[String, String](AggregateLibrary.StringAgg, List(LiteralNode(delimiter)))

    // Aggregate Functions for Statistics
    case class Corr() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.Corr)
    case class CovarPop() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.CovarPop)
    case class CovarSamp() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.CovarSamp)
    case class RegrAvgX() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.RegrAvgX)
    case class RegrAvgY() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.RegrAvgY)
    case class RegrCount() extends BinaryAggFuncPartsBasic[Double, Long](AggregateLibrary.RegrCount)
    case class RegrIntercept() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.RegrIntercept)
    case class RegrR2() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.RegrR2)
    case class RegrSlope() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.RegrSlope)
    case class RegrSXX() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.RegrSXX)
    case class RegrSXY() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.RegrSXY)
    case class RegrSYY() extends BinaryAggFuncPartsBasic[Double, Double](AggregateLibrary.RegrSYY)

    case class StdDev[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.StdDev)
    case class StdDevPop[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.StdDevPop)
    case class StdDevSamp[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.StdDevSamp)
    case class Variance[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.Variance)
    case class VarPop[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.VarPop)
    case class VarSamp[T]() extends UnaryAggFuncPartsBasic[T, T](AggregateLibrary.VarSamp)
  }
}
