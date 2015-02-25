package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{PositionedParameters, SetParameter, PositionedResult, JdbcType}

case class TsVector(value: String)
case class TsQuery(value: String)

trait PgSearchSupport extends search.PgSearchExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._

  trait SearchAssistants extends BaseSearchAssistants[TsVector, TsQuery]

  /// alias
  trait SearchImplicits extends SimpleSearchImplicits

  trait SimpleSearchImplicits {
    implicit val simpleTsVectorTypeMapper = new GenericJdbcType[TsVector]("tsvector", TsVector, _.value)
    implicit val simpleTsQueryTypeMapper = new GenericJdbcType[TsQuery]("tsquery", TsQuery, _.value)

    implicit def simpleTsVectorColumnExtensionMethods(c: Rep[TsVector])(
      implicit tm: JdbcType[TsVector], tm1: JdbcType[TsQuery]) = {
        new TsVectorColumnExtensionMethods[TsVector, TsQuery, TsVector](c)
      }
    implicit def simpleTsVectorOptionColumnExtensionMethods(c: Rep[Option[TsVector]])(
      implicit tm: JdbcType[TsVector], tm1: JdbcType[TsQuery]) = {
        new TsVectorColumnExtensionMethods[TsVector, TsQuery, Option[TsVector]](c)
      }
    implicit def simpleTsQueryColumnExtensionMethods(c: Rep[TsQuery])(
      implicit tm: JdbcType[TsVector], tm1: JdbcType[TsQuery]) = {
        new TsQueryColumnExtensionMethods[TsVector, TsQuery, TsQuery](c)
      }
    implicit def simpleTsQueryOptionColumnExtensionMethods(c: Rep[Option[TsQuery]])(
      implicit tm: JdbcType[TsVector], tm1: JdbcType[TsQuery]) = {
        new TsQueryColumnExtensionMethods[TsVector, TsQuery, Option[TsQuery]](c)
      }
  }

  trait SimpleSearchPlainImplicits {

    implicit class PgSearchPositionedResult(r: PositionedResult) {
      def nextTsVector() = nextTsVectorOption().orNull
      def nextTsVectorOption() = r.nextStringOption().map(TsVector)
      def nextTsQuery() = nextTsQueryOption().orNull
      def nextTsQueryOption() = r.nextStringOption().map(TsQuery)
    }

    implicit object SetTsVector extends SetParameter[TsVector] {
      def apply(v: TsVector, pp: PositionedParameters) = setTsVector(Option(v), pp)
    }
    implicit object SetTsVectorOption extends SetParameter[Option[TsVector]] {
      def apply(v: Option[TsVector], pp: PositionedParameters) = setTsVector(v, pp)
    }
    ///
    implicit object SetTsQuery extends SetParameter[TsQuery] {
      def apply(v: TsQuery, pp: PositionedParameters) = setTsQuery(Option(v), pp)
    }
    implicit object SetTsQueryOption extends SetParameter[Option[TsQuery]] {
      def apply(v: Option[TsQuery], pp: PositionedParameters) = setTsQuery(v, pp)
    }

    ///
    private def setTsVector(v: Option[TsVector], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject("tsvector", v.value), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
    private def setTsQuery(v: Option[TsQuery], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject("tsquery", v.value), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
  }
}
