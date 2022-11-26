package com.github.tminglei.slickpg

import java.util.UUID

import slick.SlickException
import slick.ast._
import slick.compiler.CompilerState
import slick.dbio.{Effect, NoStream}
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
  override def createColumnDDLBuilder(column: FieldSymbol, table: Table[_]): ColumnDDLBuilder = new ColumnDDLBuilder(column)

  override def createModelBuilder(tables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext): JdbcModelBuilder =
    new ExModelBuilder(tables, ignoreInvalidDefaults)

  protected lazy val useNativeUpsert = capabilities contains JdbcCapabilities.insertOrUpdate
  override protected lazy val useTransactionForUpsert = !useNativeUpsert
  override protected lazy val useServerSideUpsertReturning = useNativeUpsert

  trait ColumnOptions extends super.ColumnOptions {
    val AutoIncSeq = ExPostgresProfile.ColumnOption.AutoIncSeq
    def AutoIncSeqName(name: String) = ExPostgresProfile.ColumnOption.AutoIncSeqName(name)
    def AutoIncSeqFn(nextValFn: String => String) = ExPostgresProfile.ColumnOption.AutoIncSeqFn(nextValFn)
  }

  override val columnOptions: ColumnOptions = new ColumnOptions {}

  override val api: API = new API {}

  ///--
  trait API extends super.API {
    type InheritingTable = driver.InheritingTable

    val Over = window.Over()
    val RowCursor = window.RowCursor

    implicit class AggFuncOver[R: TypedType](aggFunc: agg.AggFuncRep[R]) {
      def over = window.WindowFuncRep[R](aggFunc._parts.toNode(implicitly[TypedType[R]]))
    }
    ///
    implicit def multiUpsertExtensionMethods[U, C[_]](q: Query[_, U, C]): InsertActionComposerImpl[U] =
      new InsertActionComposerImpl[U](compileInsert(q.toNode))
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
    private lazy val insertNames = insertingSyms.map { fs => quoteIdentifier(fs.name) }
    private lazy val pkNames = pkSyms.map { fs => quoteIdentifier(fs.name) }
    private lazy val softNames = softSyms.map { fs => quoteIdentifier(fs.name) }

    override def buildInsert: InsertBuilderResult = {
      val insert = s"insert into $tableName (${insertNames.mkString(",")}) values (${insertNames.map(_ => "?").mkString(",")})"
      val conflictWithPadding = "conflict (" + pkNames.mkString(", ") + ")" +
        (/* padding */ if (nonPkAutoIncSyms.isEmpty) "" else "where ? is null or ?=?")
      val updateOrNothing = if (softNames.isEmpty) "nothing" else
        "update set " + softNames.map(n => s"$n=EXCLUDED.$n").mkString(",")
      new InsertBuilderResult(table, s"$insert on $conflictWithPadding do $updateOrNothing", syms)
    }

    override def transformMapping(n: Node) = reorderColumns(n,
      insertingSyms ++ nonPkAutoIncSyms ++ nonPkAutoIncSyms ++ nonPkAutoIncSyms)
  }

  protected class InsertActionComposerImpl[U](override val compiled: CompiledInsert)
    extends super.CountingInsertActionComposerImpl[U](compiled) {

    /** Upsert a batch of records - insert rows whose primary key is not present in
      * the table, and update rows whose primary key is present.. */
    def insertOrUpdateAll(values: Iterable[U]): ProfileAction[MultiInsertResult, NoStream, Effect.Write] =
      if (useNativeUpsert)
        new MultiInsertOrUpdateAction(values)
      else throw new IllegalStateException("Cannot insertOrUpdateAll in without native upsert capability. " +
        "Instead use DBIO.sequence(values.map(query.insertOrUpdate))")

    ///
    class MultiInsertOrUpdateAction(values: Iterable[U]) extends SimpleJdbcProfileAction[MultiInsertResult](
      "MultiInsertOrUpdateAction", Vector(compiled.upsert.sql)) {

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
              .upsert
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
      if (existed.isDefined && !existed.get.equals(scalaType)) logger.warn(
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

  class ColumnDDLBuilder(column: FieldSymbol) extends super.ColumnDDLBuilder(column) {
    protected var autoIncSeqName: String = _
    protected var autoIncFunction: String => String = _
    protected var autoIncSeq: Boolean = _

    override protected def init(): Unit = {
      autoIncSeq = false
      super.init()
    }

    override def appendColumn(sb: StringBuilder): Unit = {
      sb append quoteIdentifier(column.name) append ' '
      if (autoIncrement && !customSqlType && !autoIncSeq) {
        sb append (if (sqlType.toUpperCase == "BIGINT") "BIGSERIAL" else "SERIAL")
      } else appendType(sb)
      appendOptions(sb)
    }

    override protected def handleColumnOption(o: ColumnOption[_]): Unit = o match {
      case ExPostgresProfile.ColumnOption.AutoIncSeqName(s) => autoIncSeqName = s
      case ExPostgresProfile.ColumnOption.AutoIncSeqFn(s) => autoIncFunction = s
      case ExPostgresProfile.ColumnOption.AutoIncSeq => autoIncSeq = true
      case _ => super.handleColumnOption(o)
    }

    override protected def appendOptions(sb: StringBuilder): Unit = {
      if (defaultLiteral ne null) sb append " DEFAULT " append defaultLiteral
      if (autoIncSeq && (autoIncSeqName ne null)) {
        val d = sb append " DEFAULT "
        val nextVal = s"nextval('$autoIncSeqName'::regclass)"
        if (autoIncFunction eq null) {
          d append nextVal
        } else {
          d append autoIncFunction(nextVal)
        }
      }
      if (notNull) sb append " NOT NULL"
      if (primaryKey) sb append " PRIMARY KEY"
      if (unique) sb append " UNIQUE"
      ()
    }

    def createSequence(table: Table[?]): Iterable[String] =
      if (autoIncSeq) {
        if (autoIncSeqName eq null) {
          val seqName = s"${table.tableName}_${column.name}_seq"
          autoIncSeqName = table.schemaName.map(s => s"${quoteIdentifier(s)}.$seqName").getOrElse(seqName)
        }
        Seq(s"create sequence $autoIncSeqName")
      } else Nil

    def dropSequence: Iterable[String] =
      if (autoIncSeq)
        Seq(s"drop sequence $autoIncSeqName")
      else Nil

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

    override val createPhase1: Iterable[String] = createAutoIncSequences ++ super.createPhase1
    override val dropPhase2: Iterable[String] = super.dropPhase2 ++ dropAutoIncSequences

    protected def createAutoIncSequences: Iterable[String] = columns.flatMap { case cb: ColumnDDLBuilder =>
      cb.createSequence(table)
    }

    protected def dropAutoIncSequences: Iterable[String] = columns.flatMap { case cb: ColumnDDLBuilder =>
      cb.dropSequence
    }

    override protected def createTable(checkNotExists: Boolean): String = {
      if(table.isInstanceOf[InheritingTable]) {
        val hTable = table.asInstanceOf[InheritingTable].inherited
        val hTableNode = hTable.toNode.asInstanceOf[TableExpansion].table.asInstanceOf[TableNode]
        s"${super.createTable(checkNotExists)} inherits (${quoteTableName(hTableNode)})"
      } else super.createTable(checkNotExists)
    }
  }
}

object ExPostgresProfile extends ExPostgresProfile {
  object ColumnOption {

    /** Flag that indicates that an auto incrementing for an AutoInc column
      * will be done by explicitly creating a sequence. */
    case object AutoIncSeq extends ColumnOption[Nothing]

    /** Name of the sequence which is generated for an AutoIncSeq column.
      * It overrides the default sequence name: "$schemaName.$tableName_$columnName_seq".
      * */
    case class AutoIncSeqName(name: String) extends ColumnOption[Nothing]

    /** Function that is used for generating the next value for an AutoIncSeq column.
      * Example:
      * AutoIncSeqFn(nextVal => s"'Prefix' || $nextVal")
      * The function above will be converted to "DEFAULT 'Prefix' || nextval('$autoIncSeqName'::regclass)"
      * */
    case class AutoIncSeqFn(nextValFn: String => String) extends ColumnOption[Nothing]
  }
}