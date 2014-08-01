package demo

import scala.slick.ast.ColumnOption
import scala.slick.driver.PostgresDriver
import Config._

/**
 *  This customizes the Slick code generator. We only do simple name mappings.
 *  For a more advanced example see https://github.com/cvogt/slick-presentation/tree/scala-exchange-2013
 */
object CustomizedCodeGenerator {

  def main(args: Array[String]) = {
    // prepare database, don't forget to adjust it according to your environment
    for(script <- initScripts) {
      val cmd = s"psql -U test -d test -f /myspace/workspace/repos/slick-pg/examples/codegen-customization/src/sql/$script"
      val exec = Runtime.getRuntime().exec(cmd);
      if (exec.waitFor() == 0) {
        println(s"$script finished.")
      }
    }

    // write the generated results to file
    codegen.writeToFile(
          "demo.MyPostgresDriver", // use our customized postgres driver
          args(0),
          "demo",
          "Tables",
          "Tables.scala"
        )
  }

  val db = PostgresDriver.simple.Database.forURL(url,driver=jdbcDriver)
  // filter out desired tables
  val included = Seq("COFFEE","SUPPLIER","COFFEE_INVENTORY")
  val model = db.withSession{ implicit session =>
    val tables = PostgresDriver.defaultTables.filter(t => included contains t.name.name.toUpperCase)
    PostgresDriver.createModel( Some(tables) )
  }

  val codegen = new scala.slick.codegen.SourceCodeGenerator(model) {
    override def Table = new Table(_) { table =>
      override def Column = new Column(_) { column =>
        // customize db type -> scala type mapping, pls adjust it according to your environment
        override def rawType = model.tpe match {
          case "java.sql.Date" => "org.joda.time.LocalDate"
          case "java.sql.Time" => "org.joda.time.LocalTime"
          case "java.sql.Timestamp" => "org.joda.time.LocalDateTime"
          // currently, all types that's not built-in support were mapped to `String`
          case "String" => model.options.find(_.isInstanceOf[ColumnOption.DBType])
            .map(_.asInstanceOf[ColumnOption.DBType].dbType).map({
              case "hstore" => "Map[String, String]"
              case "geometry" => "com.vividsolutions.jts.geom.Geometry"
              case "int8[]" => "List[Long]"
              case _ =>  "String"
            }).getOrElse("String")
          case _ => super.rawType
        }
      }
    }

    // ensure to use our customized postgres driver at `import profile.simple._`
    override def packageCode(profile: String, pkg: String, container:String="Tables") : String = {
      s"""
package ${pkg}
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object ${container} extends {
  val profile = $profile
} with ${container}

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait ${container} {
  val profile: $profile
  import profile.simple._
  ${indent(code)}
}
      """.trim()
    }
  }
}