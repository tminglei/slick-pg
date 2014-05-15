package com.github.tminglei.slickpg
package utils

import org.junit._
import org.junit.Assert._
import scala.reflect.runtime.{universe => u, currentMirror => rm}

class TypeConverterTest {
  import TypeConverterTest._

  @Test
  def testCacheKey(): Unit = {
    import TypeConverters.Key

    val tpe1 = u.typeOf[T]
    val tpe2 = u.typeOf[String]

    assertEquals(Key(tpe1, tpe2).hashCode(), Key(tpe1, tpe2).hashCode())
    assertEquals(Key(tpe1, tpe2), Key(tpe1, tpe2))

    val tpea = u.typeOf[TypeConverterTest.T]
    val tpeb = u.typeOf[java.lang.String]

    assertEquals(Key(tpe1, tpe2).hashCode(), Key(tpea, tpeb).hashCode())
    assertEquals(Key(tpe1, tpe2), Key(tpea, tpeb))

    val t1 = T1(115,T(111,"test","test dd",Some("hi")),List(157), true)
    val tt = u.typeOf[T1].decl(u.TermName("t")).asTerm
    val tpet = rm.reflect(t1).reflectField(tt).symbol.typeSignature

    assertTrue(tpe1 =:= tpet)
    assertEquals(Key(tpe1, tpe2).hashCode(), Key(tpet, tpe2).hashCode())
    assertEquals(Key(tpe1, tpe2), Key(tpet, tpe2))
  }
}

object TypeConverterTest {
  // !!!NOTE: should use outer (in object) classes, instead of inner (in trait/class) classes
  case class T(id: Long, name: String, desc: String, opt: Option[String] = None)
  case class T1(id: Long, t: T, childIds: List[Long], confirm: Boolean)
}
