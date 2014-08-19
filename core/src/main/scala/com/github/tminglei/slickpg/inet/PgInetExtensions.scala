package com.github.tminglei.slickpg
package inet

import scala.slick.driver.{JdbcTypesComponent, PostgresDriver}

trait PgInetExtensions  extends JdbcTypesComponent { driver: PostgresDriver =>

}