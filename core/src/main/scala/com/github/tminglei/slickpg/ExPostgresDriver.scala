package com.github.tminglei.slickpg

import java.util.UUID

import slick.ast._
import slick.jdbc._
import slick.jdbc.meta.MTable
import slick.lifted.PrimaryKey
import slick.driver.{InsertBuilderResult, JdbcDriver, JdbcProfile, PostgresDriver}
import slick.util.Logging

import scala.concurrent.ExecutionContext
import scala.reflect.{ClassTag, classTag}

trait ExPostgresDriver extends JdbcDriver with PostgresDriver with Logging { driver =>

  override def createUpsertBuilder(node: Insert): InsertBuilder =
    if (useServerSideUpsert) new NativeUpsertBuilder(node) else new super.UpsertBuilder(node)
  override def createTableDDLBuilder(table: Table[_]): TableDDLBuilder = new TableDDLBuilder(table)
  override def createModelBuilder(tables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext): JdbcModelBuilder =
    new ExModelBuilder(tables, ignoreInvalidDefaults)

  override protected lazy val useServerSideUpsert = capabilities contains JdbcProfile.capabilities.insertOrUpdate
  override protected lazy val useTransactionForUpsert = !useServerSideUpsert
  override protected lazy val useServerSideUpsertReturning = useServerSideUpsert
  override protected lazy val useTransactionForUpsertReturning = !useServerSideUpsertReturning

  override val api: API = new API {}

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

  /***********************************************************************
    *                          for upsert support
   ***********************************************************************/

  class NativeUpsertBuilder(ins: Insert) extends super.InsertBuilder(ins) {
    private val (nonPkAutoIncSyms, insertingSyms) = syms.toSeq.partition { s =>
      s.options.contains(ColumnOption.AutoInc) && !(s.options contains ColumnOption.PrimaryKey) }
    private lazy val (pkSyms, softSyms) = insertingSyms.partition(_.options.contains(ColumnOption.PrimaryKey))
    private lazy val pkNames = pkSyms.map { fs => quoteIdentifier(fs.name) }
    private lazy val softNames = softSyms.map { fs => quoteIdentifier(fs.name) }

    override def buildInsert: InsertBuilderResult = {
      val insert = s"insert into $tableName (${insertingSyms.mkString(",")}) values (${insertingSyms.map(_ => "?").mkString(",")})"
      val onConflict = "on conflict (" + pkNames.mkString(", ") + ")"
      val doSomething = if (softNames.isEmpty) "do nothing" else "do update set " + softNames.map(n => s"$n=EXCLUDED.$n").mkString(",")
      val padding = if (nonPkAutoIncSyms.isEmpty) "" else "where ? is null or ?=?"
      new InsertBuilderResult(table, s"$insert $onConflict $doSomething $padding", syms)
    }

    override def transformMapping(n: Node) = reorderColumns(n, insertingSyms ++ nonPkAutoIncSyms ++ nonPkAutoIncSyms ++ nonPkAutoIncSyms)
  }

  /***********************************************************************
    *                          for codegen support
   ***********************************************************************/

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

  class ExModelBuilder(mTables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext)
            extends super.ModelBuilder(mTables, ignoreInvalidDefaults) {
    override def jdbcTypeToScala(jdbcType: Int, typeName: String = ""): ClassTag[_] = {
      logger.info(s"[info]\u001B[36m jdbcTypeToScala - jdbcType $jdbcType, typeName: $typeName \u001B[0m")
      pgTypeToScala.get(typeName).getOrElse(super.jdbcTypeToScala(jdbcType, typeName))
    }
  }

  /***********************************************************************
    *                          for inherit support
   ***********************************************************************/

  trait InheritingTable { sub: Table[_] =>
    val inherited: Table[_]
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