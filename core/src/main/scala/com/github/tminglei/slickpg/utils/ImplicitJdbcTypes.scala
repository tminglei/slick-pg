package com.github.tminglei.slickpg.utils

import scala.slick.jdbc.JdbcType
import scala.slick.driver.PostgresDriver

/**
 * !!!NOTE: should be internally used by slick-pg
 */
trait ImplicitJdbcTypes extends PostgresDriver.ImplicitColumnTypes {
  implicit def optJdbcType[T](implicit t: JdbcType[T]) = t.optionType
}
