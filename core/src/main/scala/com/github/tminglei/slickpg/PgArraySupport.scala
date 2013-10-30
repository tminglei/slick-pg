package com.github.tminglei.slickpg

import java.util.{Map => JMap, UUID}
import scala.slick.driver.{BasicProfile, PostgresDriver}
import scala.slick.lifted._
import scala.slick.ast.Library.{SqlOperator, SqlFunction}
import scala.slick.ast.Node
import scala.reflect.ClassTag
import scala.slick.session.{PositionedResult, PositionedParameters}

trait PgArraySupport { driver: PostgresDriver =>

  trait ArrayImplicits {
    /** for type/name, @see [[org.postgresql.core.Oid]] and [[org.postgresql.jdbc2.TypeInfoCache]]*/
    implicit val uuidListTypeMapper = new ArrayListTypeMapper[UUID]("uuid")
    implicit val strListTypeMapper = new ArrayListTypeMapper[String]("text")
    implicit val longListTypeMapper = new ArrayListTypeMapper[Long]("int8")
    implicit val intListTypeMapper = new ArrayListTypeMapper[Int]("int4")
    implicit val floatListTypeMapper = new ArrayListTypeMapper[Float]("float8")
    implicit val boolListTypeMapper = new ArrayListTypeMapper[Boolean]("bool")
    implicit val dateListTypeMapper = new ArrayListTypeMapper[java.sql.Date]("date")
    implicit val timeListTypeMapper = new ArrayListTypeMapper[java.sql.Time]("time")
    implicit val tsListTypeMapper = new ArrayListTypeMapper[java.sql.Timestamp]("timestamp")

    ///
    implicit def arrayColumnExtensionMethods[B1](c: Column[List[B1]])(
      implicit tm: TypeMapper[B1], tm1: ArrayListTypeMapper[B1]) = {
        new ArrayListColumnExtensionMethods[B1, List[B1]](c)
    	}
    implicit def arrayOptionColumnExtensionMethods[B1](c: Column[Option[List[B1]]])(
      implicit tm: TypeMapper[B1], tm1: ArrayListTypeMapper[B1]) = {
        new ArrayListColumnExtensionMethods[B1, Option[List[B1]]](c)
    	}
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  object ArrayLibrary {
    val Any = new SqlFunction("any")
    val All = new SqlFunction("all")
    val Concatenate = new SqlOperator("||")
    val Contains  = new SqlOperator("@>")
    val ContainedBy = new SqlOperator("<@")
    val Overlap = new SqlOperator("&&")

    val Length = new SqlFunction("array_length")
    val Unnest = new SqlFunction("unnest")
  }

  /** Extension methods for array Columns */
  class ArrayListColumnExtensionMethods[B0, P1](val c: Column[P1])(
          implicit tm0: TypeMapper[B0], tm: TypeMapper[List[B0]]) extends ExtensionMethods[List[B0], P1] {
    /** required syntax: expression operator ANY (array expression) */
    def any[R](implicit om: o#to[B0, R]) = om(ArrayLibrary.Any.column[B0](n))
    /** required syntax: expression operator ALL (array expression) */
    def all[R](implicit om: o#to[B0, R]) = om(ArrayLibrary.All.column[B0](n))

    def @>[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[Boolean, R]) = {
    		om(ArrayLibrary.Contains.column(n, Node(e)))
    	}
    def <@:[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[Boolean, R]) = {
        om(ArrayLibrary.ContainedBy.column(Node(e), n))
      }
    def @&[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[Boolean, R]) = {
        om(ArrayLibrary.Overlap.column(n, Node(e)))
      }

    def ++[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[List[B0], R]) = {
        om(ArrayLibrary.Concatenate.column(n, Node(e)))
      }
    def + [P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[List[B0], R]) = {
        om(ArrayLibrary.Concatenate.column(n, Node(e)))
      }
    def +:[P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[List[B0], R]) = {
        om(ArrayLibrary.Concatenate.column(Node(e), n))
      }
    def length(dim: Column[Int] = ConstColumn(1)) = ArrayLibrary.Length.column[Int](n, Node(dim))
    def unnest[R](implicit om: o#to[B0, R]) = om(ArrayLibrary.Unnest.column(n))
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  class ArrayListTypeMapper[T: ClassTag](baseTypeName: String)
  					extends TypeMapperDelegate[List[T]] with BaseTypeMapper[List[T]] {

    def apply(v1: BasicProfile): TypeMapperDelegate[List[T]] = this

    //----------------------------------------------------------
    def zero: List[T] = Nil

    def sqlType: Int = java.sql.Types.ARRAY

    def sqlTypeName: String = s"$baseTypeName ARRAY"

    def setValue(v: List[T], p: PositionedParameters) = p.setObject(mkArray(v), sqlType)

    def setOption(v: Option[List[T]], p: PositionedParameters) = p.setObjectOption(v.map(mkArray), sqlType)

    def nextValue(r: PositionedResult): List[T] = {
      r.nextObjectOption().map(_.asInstanceOf[java.sql.Array])
        .map(_.getArray.asInstanceOf[Array[Any]].map(_.asInstanceOf[T]).toList)
        .getOrElse(zero)
    }

    def updateValue(v: List[T], r: PositionedResult) = r.updateObject(mkArray(v))

    override def valueToSQLLiteral(v: List[T]) = mkArray(v).toString

    //--
    private def mkArray(v: List[T]): java.sql.Array = new SimpleArray(v, baseTypeName)
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  /** should only be used to transfer array data into driver/preparedStatement */
  class SimpleArray(arr: Seq[Any], baseTypeName: String) extends java.sql.Array {

    def getBaseTypeName = baseTypeName

    def getBaseType = ???

    def getArray = arr.toArray

    def getArray(map: JMap[String, Class[_]]) = ???

    def getArray(index: Long, count: Int) = ???

    def getArray(index: Long, count: Int, map: JMap[String, Class[_]]) = ???

    def getResultSet = ???

    def getResultSet(map: JMap[String, Class[_]]) = ???

    def getResultSet(index: Long, count: Int) = ???

    def getResultSet(index: Long, count: Int, map: JMap[String, Class[_]]) = ???

    override def toString = buildStr(arr).toString()

    def free() = { /* nothing to do */ }

    /** copy from [[org.postgresql.jdbc4.AbstractJdbc4Connection#createArrayOf(..)]]
      * and [[org.postgresql.jdbc2.AbstractJdbc2Array#escapeArrayElement(..)]] */
    private def buildStr(elements: Seq[Any]): StringBuilder = {
      def escape(s: String) = {
        StringBuilder.newBuilder + '"' appendAll (
          s map {
            c => if (c == '"' || c == '\\') '\\' else c
          }) + '"'
      }

      StringBuilder.newBuilder + '{' append (
        elements map {
          case arr: Seq[Any] => buildStr(arr)
          case o: Any if (o != null) => escape(o.toString)
          case _ => "NULL"
        } mkString(",")) + '}'
    }
  }
}
