package com.github.tminglei.slickpg

import java.util.UUID

import slick.ast._
import slick.jdbc._
import slick.jdbc.meta.MTable
import slick.lifted.PrimaryKey
import slick.driver.{PostgresDriver, JdbcDriver}
import slick.util.Logging

import scala.concurrent.ExecutionContext
import scala.reflect.{classTag, ClassTag}

trait ExPostgresDriver extends JdbcDriver with PostgresDriver with Logging { driver =>

  override val api = new API {}
  override def createTableDDLBuilder(table: Table[_]): TableDDLBuilder = new TableDDLBuilder(table)
  override def createModelBuilder(tables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext): JdbcModelBuilder =
    new ExModelBuilder(tables, ignoreInvalidDefaults)

  private var pgTypeToScala = Map.empty[String, ClassTag[_]]

  /** NOTE: used to support code gen */
  def bindPgTypeToScala(pgType: String, scalaType: ClassTag[_]) = {
    logger.info(s"\u001B[36m >>> binding $pgType -> $scalaType \u001B[0m")
    val existed = pgTypeToScala.get(pgType)
    if (existed.isDefined) logger.warn(
      s"\u001B[31m >>> DUPLICATED BINDING - existed: ${existed.get}, new: $scalaType !!! \u001B[36m If it's expected, pls ignore it.\u001B[0m"
    )
    pgTypeToScala += (pgType -> scalaType)
  }

  {
    bindPgTypeToScala("uuid", classTag[UUID])
    bindPgTypeToScala("text", classTag[String])
    bindPgTypeToScala("bool", classTag[Boolean])
  }

  ///--
  trait API extends super.API {
    type InheritingTable = driver.InheritingTable

    /** NOTE: Array[Byte] maps to `bytea` instead of `byte ARRAY` */
    implicit val getByteArray = new GetResult[Array[Byte]] {
      def apply(pr: PositionedResult) = pr.nextBytes()
    }
    implicit val getByteArrayOption = new GetResult[Option[Array[Byte]]] {
      def apply(pr: PositionedResult) = pr.nextBytesOption()
    }
    implicit val setByteArray = new SetParameter[Array[Byte]] {
      def apply(v: Array[Byte], pp: PositionedParameters) = pp.setBytes(v)
    }
    implicit val setByteArrayOption = new SetParameter[Option[Array[Byte]]] {
      def apply(v: Option[Array[Byte]], pp: PositionedParameters) = pp.setBytesOption(v)
    }
  }

  trait InheritingTable { sub: Table[_] =>
    val inherited: Table[_]
  }

  class ExModelBuilder(mTables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext)
            extends super.ModelBuilder(mTables, ignoreInvalidDefaults) {
    override def jdbcTypeToScala(jdbcType: Int, typeName: String = ""): ClassTag[_] = {
      logger.info(s"[info]\u001B[36m jdbcTypeToScala - jdbcType $jdbcType, typeName: $typeName \u001B[0m")
      pgTypeToScala.get(typeName).getOrElse(super.jdbcTypeToScala(jdbcType, typeName))
    }
  }

  class TableDDLBuilder(table: Table[_]) extends super.TableDDLBuilder(table) {
    override protected val columns: Iterable[ColumnDDLBuilder] = {
      (if(table.isInstanceOf[InheritingTable]) {
        val hColumns = table.asInstanceOf[InheritingTable].inherited.create_*.toSeq.map(_.name.toLowerCase)
        table.create_*.filterNot(s => hColumns.contains(s.name.toLowerCase))
      } else table.create_*)
        .map(fs => createColumnDDLBuilder(fs, table))
    }
    override protected val primaryKeys: Iterable[PrimaryKey] = {
      if(table.isInstanceOf[InheritingTable]) {
        val hTable = table.asInstanceOf[InheritingTable].inherited
        val hPrimaryKeys = hTable.primaryKeys.map(pk => PrimaryKey(table.tableName + "_" + pk.name, pk.columns))
        hTable.create_*.find(_.options.contains(ColumnOption.PrimaryKey))
          .map(s => PrimaryKey(table.tableName + "_PK", IndexedSeq(Select(tableNode, s))))
          .map(Iterable(_) ++ hPrimaryKeys ++ table.primaryKeys)
          .getOrElse(hPrimaryKeys ++ table.primaryKeys)
      } else table.primaryKeys
    }

    override protected def createTable: String = {
      if(table.isInstanceOf[InheritingTable]) {
        val hTable = table.asInstanceOf[InheritingTable].inherited
        val hTableNode = hTable.toNode.asInstanceOf[TableExpansion].table.asInstanceOf[TableNode]
        s"${super.createTable} inherits (${quoteTableName(hTableNode)})"
      } else super.createTable
    }
  }
}

object ExPostgresDriver extends ExPostgresDriver