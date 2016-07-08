package com.github.tminglei.slickpg
package window

import slick.ast.Library.SqlFunction
import slick.ast.LiteralNode
import slick.jdbc.{JdbcType, JdbcTypesComponent, PostgresProfile}
import slick.lifted.OptionMapperDSL

trait PgWindowFuncSupport extends JdbcTypesComponent { driver: PostgresProfile =>
  import driver.api._

  object WindowLibrary {
    val RowNumber = new SqlFunction("row_number")
    val Rank = new SqlFunction("rank")
    val DenseRank = new SqlFunction("dense_rank")
    val PercentRank = new SqlFunction("percent_rank")
    val CumeDist = new SqlFunction("cume_dist")
    val Ntile = new SqlFunction("ntile")
    val Lag = new SqlFunction("lag")
    val Lead = new SqlFunction("lead")
    val FirstValue = new SqlFunction("first_value")
    val LastValue = new SqlFunction("last_value")
    val NthValue = new SqlFunction("nth_value")
  }

  trait WindowFunctions {
    def rowNumber() = WindowFunc[Long](WindowLibrary.RowNumber, Nil)
    def rank() = WindowFunc[Long](WindowLibrary.Rank, Nil)
    def denseRank() = WindowFunc[Long](WindowLibrary.DenseRank, Nil)
    def percentRank() = WindowFunc[Double](WindowLibrary.PercentRank, Nil)
    def cumeDist() = WindowFunc[Double](WindowLibrary.CumeDist, Nil)
    def ntile[P,R](c: Rep[P])(implicit tm: JdbcType[Int], om: OptionMapperDSL.arg[Int,P]#to[Int,R]) =
      WindowFunc[Int](WindowLibrary.Ntile, List(c.toNode))
    def lag[T](c: Rep[Option[T]], offset: Option[Int] = None, default: Option[T] = None)(implicit tm: JdbcType[T], tm1: JdbcType[Int]) =
      WindowFunc[Option[T]](WindowLibrary.Lag, List(c.toNode, LiteralNode(tm1, offset.getOrElse(1)), LiteralNode(tm, default.getOrElse(null.asInstanceOf[T]))))
    def lead[T](c: Rep[Option[T]], offset: Option[Int] = None, default: Option[T] = None)(implicit tm: JdbcType[T], tm1: JdbcType[Int]) =
      WindowFunc[Option[T]](WindowLibrary.Lead, List(c.toNode, LiteralNode(tm1, offset.getOrElse(1)), LiteralNode(tm, default.getOrElse(null.asInstanceOf[T]))))
    def firstValue[T](c: Rep[Option[T]])(implicit tm: JdbcType[T]) =
      WindowFunc[T](WindowLibrary.FirstValue, List(c.toNode))
    def lastValue[T](c: Rep[Option[T]])(implicit tm: JdbcType[T]) =
      WindowFunc[T](WindowLibrary.LastValue, List(c.toNode))
    def nthValue[T](c: Rep[Option[T]], nth: Int)(implicit tm: JdbcType[T], tm1: JdbcType[Int]) =
      WindowFunc[Option[T]](WindowLibrary.NthValue, List(c.toNode, LiteralNode(tm1, nth)))
  }
}

object PgWindowFuncSupport extends PgWindowFuncSupport with PostgresProfile {
  val WindowFunctions = new WindowFunctions {}
}