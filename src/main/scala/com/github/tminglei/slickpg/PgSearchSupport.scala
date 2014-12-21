package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.JdbcType
import scala.slick.lifted.Column

case class TsVector(value: String)
case class TsQuery(value: String)

trait PgSearchSupport extends search.PgSearchExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.Implicit._

  trait SearchAssistants extends BaseSearchAssistants[TsVector, TsQuery]

  /// alias
  trait SearchImplicits extends SimpleSearchImplicits

  trait SimpleSearchImplicits {
    implicit val simpleTsVectorTypeMapper = new GenericJdbcType[TsVector]("tsvector", TsVector, _.value)
    implicit val simpleTsQueryTypeMapper = new GenericJdbcType[TsQuery]("tsquery", TsQuery, _.value)

    implicit def simpleTsVectorColumnExtensionMethods(c: Column[TsVector])(
      implicit tm: JdbcType[TsVector], tm1: JdbcType[TsQuery]) = {
        new TsVectorColumnExtensionMethods[TsVector, TsQuery, TsVector](c)
      }
    implicit def simpleTsVectorOptionColumnExtensionMethods(c: Column[Option[TsVector]])(
      implicit tm: JdbcType[TsVector], tm1: JdbcType[TsQuery]) = {
        new TsVectorColumnExtensionMethods[TsVector, TsQuery, Option[TsVector]](c)
      }
    implicit def simpleTsQueryColumnExtensionMethods(c: Column[TsQuery])(
      implicit tm: JdbcType[TsVector], tm1: JdbcType[TsQuery]) = {
        new TsQueryColumnExtensionMethods[TsVector, TsQuery, TsQuery](c)
      }
    implicit def simpleTsQueryOptionColumnExtensionMethods(c: Column[Option[TsQuery]])(
      implicit tm: JdbcType[TsVector], tm1: JdbcType[TsQuery]) = {
        new TsQueryColumnExtensionMethods[TsVector, TsQuery, Option[TsQuery]](c)
      }
  }
}
