package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import utils.TypeConverters.Util._

object MyPostgresDriver extends PostgresDriver
                           with PgSprayJsonSupport
                           with array.PgArrayJdbcTypes {

  override lazy val Implicit = new Implicits with JsonImplicits
  override val simple = new Implicits with SimpleQL with JsonImplicits {
    implicit val strListTypeMapper = new ArrayListJdbcType[String]("text",
      mkArrayConvFromString[String], mkArrayConvToString[String])
  }
}
