package com.github.tminglei.slickpg

import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}
import scala.reflect.classTag

/** simple tsvector/tsquery string wrapper */
case class TsVector(value: String)
case class TsQuery(value: String)

trait PgSearchSupport extends search.PgSearchExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._

  trait SearchAssistants extends BaseSearchAssistants[TsVector, TsQuery]

  trait SearchCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("tsvector", classTag[TsVector])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("tsquery", classTag[TsQuery])
    }
  }

  /// alias
  trait SearchImplicits extends SimpleSearchImplicits

  trait SimpleSearchImplicits extends SearchCodeGenSupport {
    implicit val simpleTsVectorTypeMapper: JdbcType[TsVector] = new GenericJdbcType[TsVector]("tsvector", TsVector, _.value)
    implicit val simpleTsQueryTypeMapper: JdbcType[TsQuery] = new GenericJdbcType[TsQuery]("tsquery", TsQuery, _.value)

    implicit def simpleTsVectorColumnExtensionMethods(c: Rep[TsVector]) = {
        new TsVectorColumnExtensionMethods[TsVector, TsQuery, TsVector](c)
      }
    implicit def simpleTsVectorOptionColumnExtensionMethods(c: Rep[Option[TsVector]]) = {
        new TsVectorColumnExtensionMethods[TsVector, TsQuery, Option[TsVector]](c)
      }
    implicit def simpleTsQueryColumnExtensionMethods(c: Rep[TsQuery]) = {
        new TsQueryColumnExtensionMethods[TsVector, TsQuery, TsQuery](c)
      }
    implicit def simpleTsQueryOptionColumnExtensionMethods(c: Rep[Option[TsQuery]]) = {
        new TsQueryColumnExtensionMethods[TsVector, TsQuery, Option[TsQuery]](c)
      }
  }

  trait SimpleSearchPlainImplicits extends SearchCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgSearchPositionedResult(r: PositionedResult) {
      def nextTsVector() = nextTsVectorOption().orNull
      def nextTsVectorOption() = r.nextStringOption().map(TsVector)
      def nextTsQuery() = nextTsQueryOption().orNull
      def nextTsQueryOption() = r.nextStringOption().map(TsQuery)
    }

    ////////////////////////////////////////////////////////////////
    implicit val getTsVector = mkGetResult(_.nextTsVector())
    implicit val getTsVectorOption = mkGetResult(_.nextTsVectorOption())
    implicit val setTsVector = mkSetParameter[TsVector]("tsvector", _.value)
    implicit val setTsVectorOption = mkOptionSetParameter[TsVector]("tsvector", _.value)

    implicit val getTsQuery = mkGetResult(_.nextTsQuery())
    implicit val getTsQueryOption = mkGetResult(_.nextTsQueryOption())
    implicit val setTsQuery = mkSetParameter[TsQuery]("tsquery", _.value)
    implicit val setTsQueryOption = mkOptionSetParameter[TsQuery]("tsquery", _.value)
  }
}
