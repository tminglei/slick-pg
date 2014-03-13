package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import utils.TypeConverters.Util._

object MyPostgresDriver extends PostgresDriver
                           with PgSprayJsonSupport
                           with array.PgArrayJavaTypes {

  override val Implicit = new Implicits with JsonImplicits
  override val simple = new Implicits with SimpleQL with JsonImplicits {
    implicit val strListTypeMapper = new ArrayListJavaType[String]("text",
      mkArrayConvFromString[String], mkArrayConvToString[String])
  }
}
