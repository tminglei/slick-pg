package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import utils.TypeConverters.Util._

object MyPostgresDriver extends PostgresDriver
                           with PgJson4sSupport
                           with array.PgArrayJdbcTypes {
  /// for json support
  type DOCType = text.Document
  override val jsonMethods = org.json4s.native.JsonMethods

  override val Implicit = new Implicits with JsonImplicits
  override val simple = new Implicits with SimpleQL with JsonImplicits {
    implicit val strListTypeMapper = new ArrayListJdbcType[String]("text",
      mkArrayConvFromString[String], mkArrayConvToString[String])
  }
}
