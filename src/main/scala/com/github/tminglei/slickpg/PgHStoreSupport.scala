package com.github.tminglei.slickpg

import scala.collection.convert.{WrapAsJava, WrapAsScala}
import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import org.postgresql.util.HStoreConverter
import scala.slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}

trait PgHStoreSupport extends hstore.PgHStoreExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>

  /// alias
  trait HStoreImplicits extends SimpleHStoreImplicits

  trait SimpleHStoreImplicits {
    implicit val simpleHStoreTypeMapper =
      new GenericJdbcType[Map[String, String]](
        "hstore",
        (v) => WrapAsScala.mapAsScalaMap(HStoreConverter.fromString(v).asInstanceOf[java.util.Map[String, String]]).toMap,
        (v) => HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v)),
        hasLiteralForm = false
      )

    implicit def simpleHStoreColumnExtensionMethods(c: Column[Map[String, String]])(
      implicit tm: JdbcType[Map[String, String]], tm1: JdbcType[List[String]]) = {
        new HStoreColumnExtensionMethods[Map[String, String]](c)
      }
    implicit def simpleHStoreOptionColumnExtensionMethods(c: Column[Option[Map[String,String]]])(
      implicit tm: JdbcType[Map[String, String]], tm1: JdbcType[List[String]]) = {
        new HStoreColumnExtensionMethods[Option[Map[String, String]]](c)
      }
  }

  /// static sql support, NOTE: no extension methods available for static sql usage
  trait SimpleHStorePlainImplicits {

    implicit class PgHStorePositionedResult(r: PositionedResult) {
      def nextHStore() = nextHStoreOption().getOrElse(Map.empty)
      def nextHStoreOption() = r.nextStringOption().map { v =>
        WrapAsScala.mapAsScalaMap(HStoreConverter.fromString(v).asInstanceOf[java.util.Map[String, String]]).toMap
      }
    }

    ////////////////////////////////////////////////////////////////////////
    implicit object SetHStore extends SetParameter[Map[String, String]] {
      def apply(v: Map[String, String], pp: PositionedParameters) = setHStore(Option(v), pp)
    }
    implicit object SetHStoreOption extends SetParameter[Option[Map[String, String]]] {
      def apply(v: Option[Map[String, String]], pp: PositionedParameters) = setHStore(v, pp)
    }

    ///
    private def setHStore(v: Option[Map[String,String]], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject("hstore", HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v))), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
  }
}
