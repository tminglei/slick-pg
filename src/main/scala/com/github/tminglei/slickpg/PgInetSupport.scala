package com.github.tminglei.slickpg

import java.net.InetAddress

import scala.slick.driver.PostgresDriver

trait PgInetSupport extends hstore.PgHStoreExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>

  trait InetImplicits {
    implicit val inetTypeMapper =
      new GenericJdbcType[InetAddress]("inet",
        (v) => InetAddress.getByName(v),
        (v) => v.getHostAddress,
        hasLiteralForm = false)

  }

}
