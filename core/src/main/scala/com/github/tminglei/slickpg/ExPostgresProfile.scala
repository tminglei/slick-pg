package com.github.tminglei.slickpg

import java.util.UUID

import slick.ast._
import slick.compiler.CompilerState
import slick.jdbc._
import slick.jdbc.meta.MTable
import slick.lifted.{PrimaryKey, Query, WrappingQuery}
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

    implicit def queryLateralExtensionMethods[E, U, C[_]](q: Query[E, U, C]) =
      new QueryLateralExtensionMethods(q)
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
    *          for lateral and aggregate/window function support
   *************************************************************************/
  val USE_LATERAL_JOIN = LiteralNode("__use_lateral_join")

  class QueryLateralExtensionMethods[E1, U1, C[_]](q1: Query[E1, U1, C]) {
    def lateral[E2, U2, D[_]](qt: E1 => Query[E2, U2, D], jt: JoinType = JoinType.Inner): Query[(E1, E2), (U1, U2), C] = {
      val leftGen, rightGen = new AnonSymbol
      val aliased1 = q1.shaped.encodeRef(Ref(leftGen))
      val q2 = qt(aliased1.value)
      val aliased2 = q2.shaped.encodeRef(Ref(rightGen))
      new WrappingQuery[(E1, E2), (U1, U2), C](Join(leftGen, rightGen, q1.toNode, q2.toNode, jt, USE_LATERAL_JOIN), aliased1.zip(aliased2))
    }
  }

  class QueryBuilder(tree: Node, state: CompilerState) extends super.QueryBuilder(tree, state) {
    import slick.util.MacroSupport.macroSupportInterpolation
    override protected def buildJoin(j: Join): Unit = j.on match {
      case USE_LATERAL_JOIN => {
        buildFrom(j.left, Some(j.leftGen))
        b"\n${j.jt.sqlName} join lateral "
        buildFrom(j.right, Some(j.rightGen))
        b"\non true"
      }
      case _ => super.buildJoin(j)
    }
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
      val conflictWithPadding = "conflict (" + pkNames.mkString(", ") + ")" + (/* padding */ if (nonPkAutoIncSyms.isEmpty) "" else "where ? is null or ?=?")
      val updateOrNothing = if (softNames.isEmpty) "nothing" else "update set " + softNames.map(n => s"$n=EXCLUDED.$n").mkString(",")
      new InsertBuilderResult(table, s"$insert on $conflictWithPadding do $updateOrNothing", syms)
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