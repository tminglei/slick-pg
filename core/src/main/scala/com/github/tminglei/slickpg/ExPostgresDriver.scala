package com.github.tminglei.slickpg

import scala.slick.ast.{TableNode, TableExpansion, Select, ColumnOption}
import scala.slick.lifted.PrimaryKey
import scala.slick.driver.{PostgresDriver, JdbcDriver}

trait ExPostgresDriver extends JdbcDriver with PostgresDriver { driver =>

  override val simple = new SimpleQL {}
  override def createTableDDLBuilder(table: Table[_]): TableDDLBuilder = new TableDDLBuilder(table)

  trait SimpleQL extends super.SimpleQL {
    type InheritingTable = driver.InheritingTable
  }

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