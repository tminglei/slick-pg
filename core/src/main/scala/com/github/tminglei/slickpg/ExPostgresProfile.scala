package com.github.tminglei.slickpg

import java.util.UUID

import slick.ast._
import slick.compiler.CompilerState
import slick.jdbc._
import slick.jdbc.meta.MTable
import slick.lifted.PrimaryKey
import slick.util.Logging

import scala.concurrent.ExecutionContext
import scala.reflect.{ClassTag, classTag}

trait ExPostgresProfile extends JdbcProfile with PostgresProfile with Logging { driver =>

  override def createQueryBuilder(n: Node, state: CompilerState): QueryBuilder = new QueryBuilder(n, state)
  override def createUpsertBuilder(node: Insert): InsertBuilder =
    if (useNativeUpsert) new NativeUpsertBuilder(node) else new super.UpsertBuilder(node)
  override def createTableDDLBuilder(table: Table[_]): TableDDLBuilder = new TableDDLBuilder(table)
  override def createModelBuilder(tables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext): JdbcModelBuilder =
    new ExModelBuilder(tables, ignoreInvalidDefaults)

  protected lazy val useNativeUpsert = capabilities contains JdbcCapabilities.insertOrUpdate
  override protected lazy val useTransactionForUpsert = !useNativeUpsert
  override protected lazy val useServerSideUpsertReturning = useNativeUpsert

  override val api: API = new API {}

  ///--
  trait API extends super.API {
    type InheritingTable = driver.InheritingTable

    val Over = window.Over()
    val RowCursor = window.RowCursor

    implicit class AggFuncOver[R: TypedType](aggFunc: agg.AggFuncRep[R]) {
      def over = window.WindowFuncRep[R](aggFunc._parts.toNode(implicitly[TypedType[R]]))
    }

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

  /*************************************************************************
    *                 for aggregate and window function support
   *************************************************************************/

  class QueryBuilder(tree: Node, state: CompilerState) extends super.QueryBuilder(tree, state) {
    import slick.util.MacroSupport.macroSupportInterpolation
    override def expr(n: Node, skipParens: Boolean = false) = n match {
      case agg.AggFuncExpr(func, params, orderBy, filter, distinct, forOrdered) =>
        if (func == Library.CountAll) b"${func.name}"
        else {
          b"${func.name}("
          if (distinct) b"distinct "
          b.sep(params, ",")(expr(_, true))
          if (orderBy.nonEmpty && !forOrdered) buildOrderByClause(orderBy)
          b")"
        }
        if (orderBy.nonEmpty && forOrdered) { b" within group ("; buildOrderByClause(orderBy); b")" }
        if (filter.isDefined) { b" filter ("; buildWhereClause(filter); b")" }
      case window.WindowFuncExpr(aggFuncExpr, partitionBy, orderBy, frameDef) =>
        expr(aggFuncExpr)
        b" over ("
        if(partitionBy.nonEmpty) { b" partition by "; b.sep(partitionBy, ",")(expr(_, true)) }
        if(orderBy.nonEmpty) buildOrderByClause(orderBy)
        frameDef.map {
          case (mode, start, Some(end)) => b" $mode between $start and $end"
          case (mode, start, None)      => b" $mode $start"
        }
        b")"
      case _ => super.expr(n, skipParens)
    }
  }

  /***********************************************************************
    *                          for upsert support
   ***********************************************************************/

  class NativeUpsertBuilder(ins: Insert) extends super.InsertBuilder(ins) {
    /* NOTE: pk defined by using method `primaryKey` and pk defined with `PrimaryKey` can only have one,
             here we let table ddl to help us ensure this. */
    private lazy val funcDefinedPKs = table.profileTable.asInstanceOf[Table[_]].primaryKeys
    private lazy val (nonPkAutoIncSyms, insertingSyms) = syms.toSeq.partition { s =>
      s.options.contains(ColumnOption.AutoInc) && !(s.options contains ColumnOption.PrimaryKey) }
    private lazy val (pkSyms, softSyms) = insertingSyms.partition { sym =>
      sym.options.contains(ColumnOption.PrimaryKey) || funcDefinedPKs.exists(pk => pk.columns.collect {
        case Select(_, f: FieldSymbol) => f
      }.exists(_.name == sym.name)) }
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
    pgTypeToScala.synchronized {
      val existed = pgTypeToScala.get(pgType)
      if (existed.isDefined) logger.warn(
        s"\u001B[31m >>> DUPLICATED binding for $pgType - existed: ${existed.get}, new: $scalaType !!! \u001B[36m If it's expected, pls ignore it.\u001B[0m"
      )
      pgTypeToScala += (pgType -> scalaType)
    }
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

object ExPostgresProfile extends ExPostgresProfile