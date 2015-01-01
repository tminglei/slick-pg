package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.{PositionedParameters, PositionedResult, JdbcType}
import scala.slick.lifted.Column

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

    implicit def simpleInetColumnExtensionMethods(c: Column[InetString])(
      implicit tm: JdbcType[InetString]) = {
        new InetColumnExtensionMethods[InetString, InetString](c)
      }
    implicit def simpleInetOptionColumnExtensionMethods(c: Column[Option[InetString]])(
      implicit tm: JdbcType[InetString]) = {
        new InetColumnExtensionMethods[InetString, Option[InetString]](c)
      }

    implicit def simpleMacAddrColumnExtensionMethods(c: Column[MacAddrString])(
      implicit tm: JdbcType[MacAddrString]) = {
        new MacAddrColumnExtensionMethods[MacAddrString, MacAddrString](c)
      }
    implicit def simpleMacAddrOptionColumnExtensionMethods(c: Column[Option[MacAddrString]])(
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
    implicit class PgNetPositionedParameters(p: PositionedParameters) {
      def setIPAddr(v: InetString) = setIPAddrOption(Option(v))
      def setIPAddrOption(v: Option[InetString]) = {
        p.pos += 1
        v match {
          case Some(v) => p.ps.setObject(p.pos, utils.mkPGobject("inet", v.value))
          case None    => p.ps.setNull(p.pos, java.sql.Types.OTHER)
        }
      }
      def setMacAddr(v: MacAddrString) = setMacAddrOption(Option(v))
      def setMacAddrOption(v: Option[MacAddrString]) = {
        p.pos += 1
        v match {
          case Some(v) => p.ps.setObject(p.pos, utils.mkPGobject("macaddr", v.value))
          case None    => p.ps.setNull(p.pos, java.sql.Types.OTHER)
        }
      }
    }
  }
}
