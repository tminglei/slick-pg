package com.github.tminglei.slickpg.str

import slick.jdbc.PostgresProfile

/**
  * Created by minglei on 12/12/16.
  */
trait PgStringSupport extends PgStringExtensions { driver: PostgresProfile =>
  import driver.api._

  trait PgStringImplicits {
    implicit def pgStringColumnExtensionMethods(c: Rep[String]) = new PgStringColumnExtensionMethods[String](c)
    implicit def pgStringOptionColumnExtensionMethods(c: Rep[Option[String]]) = new PgStringColumnExtensionMethods[Option[String]](c)
    implicit def pgStringByteaColumnExtensionMethods(c: Rep[Array[Byte]]) = new PgStringByteaColumnExtensionMethods[Array[Byte]](c)
    implicit def pgStringByteaOptionColumnExtensionMethods(c: Rep[Option[Array[Byte]]]) = new PgStringByteaColumnExtensionMethods[Option[Array[Byte]]](c)
  }
}