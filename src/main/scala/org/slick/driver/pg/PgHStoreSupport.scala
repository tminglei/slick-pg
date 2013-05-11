package org.slick.driver.pg

import scala.slick.driver.{BasicProfile, PostgresDriver}
import scala.slick.lifted._
import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.ast.{Library, Node}
import scala.slick.session.{PositionedResult, PositionedParameters}
import scala.collection.convert.{WrapAsJava, WrapAsScala}
import org.postgresql.util.PGobject

trait PgHStoreSupport { driver: PostgresDriver =>

  trait HStoreImplicits {
    implicit val hstoreMapTypeMapper = new HStoreMapTypeMapper

    implicit def hstoreMapColumnExtensionMethods(c: Column[Map[String, String]])(
      implicit tm: TypeMapper[Map[String, String]], tm1: TypeMapper[List[String]]) = {
    		new HStoreColumnExtensionMethods[Map[String, String]](c)
    	}
    implicit def hstoreMapOptionColumnExtensionMethods(c: Column[Option[Map[String,String]]])(
      implicit tm: TypeMapper[Map[String, String]], tm1: TypeMapper[List[String]]) = {
    		new HStoreColumnExtensionMethods[Option[Map[String, String]]](c)
    	}
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  object HStoreLibrary {
    val On = new SqlOperator("->")
    val Exist   = new SqlFunction("exist")
//    val ExistAll = new SqlOperator("?&")  //can't support, because there exists '?' conflict
//    val ExistAny = new SqlOperator("?|")  //can't support, because there exists '?' conflict
    val Defined = new SqlFunction("defined")
    val Contains = new SqlOperator("@>")
    val ContainedBy = new SqlOperator("<@")

    val Concatenate = new SqlOperator("||")
    val Delete = new SqlOperator("-")
  }

  /** Extension methods for hstore Columns */
  class HStoreColumnExtensionMethods[P1](val c: Column[P1])(
            implicit tm: TypeMapper[Map[String, String]], tm1: TypeMapper[List[String]])
                extends ExtensionMethods[Map[String, String], P1] {

    def +>[P2, R](k: Column[P2])(implicit om: o#arg[String, P2]#to[String, R]) = {
        om(HStoreLibrary.On.column[String](n, Node(k)))
      }
    def >>[T: TypeMapper](k: Column[String]) = {
        Library.Cast.column[T](HStoreLibrary.On(n, Node(k)))
      }
    def ??[P2, R](k: Column[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om(HStoreLibrary.Exist.column(n, Node(k)))
      }
    def ?&[P2, R](k: Column[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om(HStoreLibrary.Defined.column(n, Node(k)))
      }
    def @>[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Boolean, R]) = {
        om(HStoreLibrary.Contains.column(n, Node(c2)))
      }
    def <@:[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Boolean, R]) = {
        om(HStoreLibrary.ContainedBy.column(Node(c2), n))
      }

    def @+[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Map[String, String], R]) = {
        om(HStoreLibrary.Concatenate.column[Map[String, String]](n, Node(c2)))
      }
    def @-[P2, R](c2: Column[P2])(implicit om: o#arg[String, P2]#to[Map[String, String], R]) = {
        om(HStoreLibrary.Delete.column[Map[String, String]](n, Node(c2)))
      }
    def --[P2, R](c2: Column[P2])(implicit om: o#arg[List[String], P2]#to[Map[String, String], R]) = {
        om(HStoreLibrary.Delete.column[Map[String, String]](n, Node(c2)))
      }
    def -/[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Map[String, String], R]) = {
        om(HStoreLibrary.Delete.column[Map[String, String]](n, Node(c2)))
      }
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  class HStoreMapTypeMapper extends TypeMapperDelegate[Map[String, String]] with BaseTypeMapper[Map[String, String]] {

    def apply(v1: BasicProfile): TypeMapperDelegate[Map[String, String]] = this

    //----------------------------------------------------------
    def zero = Map.empty[String, String]

    def sqlType = java.sql.Types.OTHER

    def sqlTypeName = "hstore"

    def setValue(v: Map[String, String], p: PositionedParameters) = p.setObject(toPGObject(v), sqlType)

    def setOption(v: Option[Map[String, String]], p: PositionedParameters) = p.setObjectOption(v.map(toPGObject), sqlType)

    def nextValue(r: PositionedResult) = {
      r.nextObjectOption().map(_.asInstanceOf[java.util.Map[String, String]])
        .map(WrapAsScala.mapAsScalaMap(_).toMap)
        .getOrElse(zero)
    }

    def updateValue(v: Map[String, String], r: PositionedResult) = r.updateObject(WrapAsJava.mapAsJavaMap(v))

    override def valueToSQLLiteral(v: Map[String, String]) = buildStr(v).toString()

    ///
    private def toPGObject(v: Map[String, String]) = {
      val obj = new PGobject
      obj.setType(sqlTypeName)
      obj.setValue(valueToSQLLiteral(v))
      obj
    }

    /** copy from [[org.postgresql.util.HStoreConverter#toString(..)]] */
    private def buildStr(m: Map[String, String]) = {
      def escape(s: String) = {
        StringBuilder.newBuilder + '"' appendAll (
          s map {
            c => if (c == '"' || c == '\\') '\\' else c
          }) + '"'
      }

      StringBuilder.newBuilder append (
        m map {
          case (k, v) => escape(k) append "=>" append Option(v).map(escape _).getOrElse("NULL")
        } mkString(",")
      )
    }
  }
}
