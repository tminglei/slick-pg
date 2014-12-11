package com.github.tminglei.slickpg

import org.postgresql.util.PGobject

package object utils {
  def mkPGobject(typeName: String, value: String) = {
    val obj = new PGobject
    obj.setType(typeName)
    obj.setValue(value)
    obj
  }
}
