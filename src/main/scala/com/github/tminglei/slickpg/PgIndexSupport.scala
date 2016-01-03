package com.github.tminglei.slickpg

import com.github.tminglei.slickpg.index.PgIndexExtensions
import slick.driver.PostgresDriver

trait PgIndexSupport extends PgIndexExtensions { driver: PostgresDriver =>

}


