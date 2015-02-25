package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}

/** simple inet string wrapper */
case class InetString(value: String) {
  lazy val isIPv6 = value.contains(":")
  lazy val address = value.split("/")(0)
  lazy val masklen: Int = {
    val parts = value.split("/")
    if (parts.length > 1) parts(1).toInt
    else if (isIPv6) 128
    else 32
  }
}

/** simple mac addr string wrapper */
case class MacAddrString(value: String)

/**
 * simple inet/macaddr support; if all you want is just getting from / saving to db, and using pg json operations/methods, it should be enough
 */
trait PgNetSupport extends net.PgNetExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._

  /// alias
  trait NetImplicits extends SimpleNetImplicits

  trait SimpleNetImplicits {
    implicit val simpleInetTypeMapper =
      new GenericJdbcType[InetString]("inet",
        (v) => InetString(v),
        (v) => v.value,
        hasLiteralForm = false
      )
    implicit val simpleMacAddrTypeMapper =
      new GenericJdbcType[MacAddrString]("macaddr",
        (v) => MacAddrString(v),
        (v) => v.value,
        hasLiteralForm = false
      )

    implicit def simpleInetColumnExtensionMethods(c: Rep[InetString])(
      implicit tm: JdbcType[InetString]) = {
        new InetColumnExtensionMethods[InetString, InetString](c)
      }
    implicit def simpleInetOptionColumnExtensionMethods(c: Rep[Option[InetString]])(
      implicit tm: JdbcType[InetString]) = {
        new InetColumnExtensionMethods[InetString, Option[InetString]](c)
      }

    implicit def simpleMacAddrColumnExtensionMethods(c: Rep[MacAddrString])(
      implicit tm: JdbcType[MacAddrString]) = {
        new MacAddrColumnExtensionMethods[MacAddrString, MacAddrString](c)
      }
    implicit def simpleMacAddrOptionColumnExtensionMethods(c: Rep[Option[MacAddrString]])(
      implicit tm: JdbcType[MacAddrString]) = {
        new MacAddrColumnExtensionMethods[MacAddrString, Option[MacAddrString]](c)
      }
  }

  trait SimpleNetPlainImplicits {

    implicit class PgNetPositionedResult(r: PositionedResult) {
      def nextIPAddr() = nextIPAddrOption().orNull
      def nextIPAddrOption() = r.nextStringOption().map(InetString)
      def nextMacAddr() = nextMacAddrOption().orNull
      def nextMacAddrOption() = r.nextStringOption().map(MacAddrString)
    }

    implicit object SetIPAddr extends SetParameter[InetString] {
      def apply(v: InetString, pp: PositionedParameters) = setIPAddr(Option(v), pp)
    }
    implicit object SetIPAddrOption extends SetParameter[Option[InetString]] {
      def apply(v: Option[InetString], pp: PositionedParameters) = setIPAddr(v, pp)
    }
    ///
    implicit object SetMacAddr extends SetParameter[MacAddrString] {
      def apply(v: MacAddrString, pp: PositionedParameters) = setMacAddr(Option(v), pp)
    }
    implicit object SetMacAddrOption extends SetParameter[Option[MacAddrString]] {
      def apply(v: Option[MacAddrString], pp: PositionedParameters) = setMacAddr(v, pp)
    }

    ///
    private def setIPAddr(v: Option[InetString], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject("inet", v.value), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
    private def setMacAddr(v: Option[MacAddrString], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject("macaddr", v.value), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
  }
}
