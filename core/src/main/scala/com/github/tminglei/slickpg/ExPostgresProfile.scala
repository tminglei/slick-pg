package com.github.tminglei.slickpg

import java.util.UUID

import slick.SlickException
import slick.ast._
import slick.compiler.{CompilerState, InsertCompiler, Phase, QueryCompiler}
import slick.dbio.{Effect, NoStream}
import slick.jdbc._
import slick.jdbc.meta.MTable
import slick.lifted.{PrimaryKey, Query}
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
  }

  trait ByteaPlainImplicits {
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

  abstract class UpsertBuilderBase(ins: Insert) extends super.InsertBuilder(ins) {
    protected lazy val funcDefinedPKs = table.profileTable.asInstanceOf[Table[_]].primaryKeys
    protected lazy val (nonPkAutoIncSyms, insertingSyms) = syms.toSeq.partition { s =>
      s.options.contains(ColumnOption.AutoInc) && !(s.options contains ColumnOption.PrimaryKey) }
    protected lazy val (pkSyms, softSyms) = insertingSyms.partition { sym =>
      sym.options.contains(ColumnOption.PrimaryKey) || funcDefinedPKs.exists(pk => pk.columns.collect {
        case Select(_, f: FieldSymbol) => f
      }.exists(_.name == sym.name)) }
    protected lazy val insertNames = insertingSyms.map { fs => quoteIdentifier(fs.name) }
    protected lazy val pkNames = pkSyms.map { fs => quoteIdentifier(fs.name) }
    protected lazy val softNames = softSyms.map { fs => quoteIdentifier(fs.name) }
  }

  class NativeUpsertBuilder(ins: Insert) extends UpsertBuilderBase(ins) {
    override def buildInsert: InsertBuilderResult = {
      val insert = s"insert into $tableName (${insertNames.mkString(",")}) values (${insertNames.map(_ => "?").mkString(",")})"
      val conflictWithPadding = "conflict (" + pkNames.mkString(", ") + ")" + (/* padding */ if (nonPkAutoIncSyms.isEmpty) "" else "where ? is null or ?=?")
      val updateOrNothing = if (softNames.isEmpty) "nothing" else "update set " + softNames.map(n => s"$n=EXCLUDED.$n").mkString(",")
      new InsertBuilderResult(table, s"$insert on $conflictWithPadding do $updateOrNothing", syms)
    }

    override def transformMapping(n: Node) = reorderColumns(n, insertingSyms ++ nonPkAutoIncSyms ++ nonPkAutoIncSyms ++ nonPkAutoIncSyms)
  }

  class MultiUpsertBuilder(ins: Insert) extends UpsertBuilderBase(ins) {
    override def buildInsert: InsertBuilderResult = {
      val start = allNames.iterator.mkString(s"insert into $tableName (", ",", ") ")
      val insert = s"$start values $allVars"
      val conflictWithPadding = "conflict (" + pkNames.mkString(", ") + ")" + (
        if (nonPkAutoIncSyms.isEmpty) ""
        else "where ? is null or ?=?"
      )
      val updateOrNothing =
        if (softNames.isEmpty) "nothing"
        else "update set " + softNames.map(n => s"$n=EXCLUDED.$n").mkString(",")
      new InsertBuilderResult(table, s"$insert on $conflictWithPadding do $updateOrNothing", syms)
    }

    override def transformMapping(n: Node) =
      reorderColumns(n, insertingSyms ++ nonPkAutoIncSyms ++ nonPkAutoIncSyms ++ nonPkAutoIncSyms)
  }

  class MultiUpsertCompiledInsert(node: Node) extends JdbcCompiledInsert(node) {
    lazy val multiUpsert = compile(multiUpsertCompiler)
  }

  implicit def multiUpsertExtensionMethods[U, C[_]](q: Query[_, U, C]): InsertActionComposerImpl[U] =
    new InsertActionComposerImpl[U](compileInsert(q.toNode))

  lazy val multiUpsertCompiler = QueryCompiler(
    Phase.assignUniqueSymbols,
    Phase.inferTypes,
    new InsertCompiler(InsertCompiler.AllColumns),
    new JdbcInsertCodeGen(insert => new MultiUpsertBuilder(insert)))

  override def compileInsert(tree: Node) = new MultiUpsertCompiledInsert(tree)
  protected class InsertActionComposerImpl[U](override val compiled: CompiledInsert)
    extends super.CountingInsertActionComposerImpl[U](compiled) {

    /** Upsert a batch of records - insert rows whose primary key is not present in
      * the table, and update rows whose primary key is present.. */
    def insertOrUpdateAll(values: Iterable[U]): ProfileAction[MultiInsertResult, NoStream, Effect.Write] =
      if (useNativeUpsert)
        new MultiInsertOrUpdateAction(values)
      else throw new IllegalStateException("Cannot insertOrUpdateAll in without native upsert capability. Instead use DBIO.sequence(values.map(query.insertOrUpdate))")
        // Below fails to compile because it returns a DBIOAction and not a ProfileAction
        // api.DBIO.sequence(values.map(insertOrUpdate)).map { insertResults: Iterable[SingleInsertOrUpdateResult] =>
        //   Option(insertResults.sum)
        // }

    class MultiInsertOrUpdateAction(values: Iterable[U]) extends SimpleJdbcProfileAction[MultiInsertResult](
      "MultiInsertOrUpdateAction",
      Vector(compiled.asInstanceOf[MultiUpsertCompiledInsert].multiUpsert.sql)) {

      private def tableHasPrimaryKey: Boolean =
        List(compiled.upsert, compiled.checkInsert, compiled.updateInsert)
          .filter(_ != null)
          .exists(artifacts =>
            artifacts.ibr.table.profileTable.asInstanceOf[Table[_]].primaryKeys.nonEmpty
              || artifacts.ibr.fields.exists(_.options.contains(ColumnOption.PrimaryKey))
          )

      if (!tableHasPrimaryKey)
        throw new SlickException("InsertOrUpdateAll is not supported on a table without PK.")

      override def run(ctx: Backend#Context, sql: Vector[String]) =
        nativeUpsert(values, sql.head)(ctx.session)

      protected def nativeUpsert(values: Iterable[U], sql: String)(
        implicit session: Backend#Session): MultiInsertResult =
        preparedInsert(sql, session) { st =>
          st.clearParameters()
          for (value <- values) {
            compiled
              .asInstanceOf[MultiUpsertCompiledInsert]
              .multiUpsert
              .converter
              .set(value, st)
            st.addBatch()
          }
          val counts = st.executeBatch()
          retManyBatch(st, values, counts)
        }
    }
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