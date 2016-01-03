package com.github.tminglei.slickpg
package index

import slick.ast.Node
import slick.driver.{PostgresDriver, JdbcTypesComponent}
import slick.lifted._

class OrderedIndex(override val name: String,
                   override val table: AbstractTable[_],
                   override val on: IndexedSeq[Node],
                   val ordering: slick.ast.Ordering, override val unique: Boolean) extends Index(name, table, on, unique)


trait PgIndexExtensions extends JdbcTypesComponent { driver: PostgresDriver =>

  import driver.api._

  trait AbstractTableIndexExtensions[T <: AbstractTable[_]] { this: AbstractTable[_] =>
    /** Define an ordered index or a ordered unique constraint. */
    def orderedIndex[T](name: String, on: T, ordering: slick.ast.Ordering, unique: Boolean = false)(implicit shape: Shape[_ <: slick.lifted.FlatShapeLevel, T, _, _]) = new OrderedIndex(name, this, ForeignKey.linearizeFieldRefs(shape.toNode(on)), ordering, unique)
  }


}
