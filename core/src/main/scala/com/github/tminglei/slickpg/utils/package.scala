package com.github.tminglei.slickpg

import org.postgresql.util.PGobject

package object utils {
  // added to support testing, shouldn't be used externally
  val dbUrl = "jdbc:postgresql://localhost/test?user=postgres"

  ///
  def mkPGobject(typeName: String, value: String) = {
    val obj = new PGobject
    obj.setType(typeName)
    obj.setValue(value)
    obj
  }
}
