package com.github.tminglei.slickpg
package utils

import org.junit._
import org.junit.Assert._
import scala.reflect.runtime.{universe => ru, currentMirror => runtimeMirror}

class TypeConverterTest {
  import TypeConverters.Util._
  import PGObjectTokenizer.PGElements._
  import TypeConverterTest._

  @Test
  def testCacheKey(): Unit = {
    import TypeConverters.CacheKey

    val tpe1 = ru.typeOf[T]
    val tpe2 = ru.typeOf[Element]

    assertEquals(CacheKey(tpe1, tpe2).hashCode(), CacheKey(tpe1, tpe2).hashCode())
    assertEquals(CacheKey(tpe1, tpe2), CacheKey(tpe1, tpe2))

    val tpea = ru.typeOf[TypeConverterTest.T]
    val tpeb = ru.typeOf[PGObjectTokenizer.PGElements.Element]

    assertEquals(CacheKey(tpe1, tpe2).hashCode(), CacheKey(tpea, tpeb).hashCode())
    assertEquals(CacheKey(tpe1, tpe2), CacheKey(tpea, tpeb))

    val t1 = T1(115,T(111,"test","test dd",Some("hi")),List(157))
    val tt = ru.typeOf[T1].declaration(ru.newTermName("t")).asTerm
    val tpet = runtimeMirror.reflect(t1).reflectField(tt).symbol.typeSignature

    assertTrue(tpe1 =:= tpet)
    assertEquals(CacheKey(tpe1, tpe2).hashCode(), CacheKey(tpet, tpe2).hashCode())
    assertEquals(CacheKey(tpe1, tpe2), CacheKey(tpet, tpe2))
  }

  @Test
  def testConverterUtil(): Unit = {
    TypeConverters.register((v: String) => v.toInt)
    TypeConverters.register((v: String) => v.toLong)
    //    Converters.register((v: String) => v)

    // simple case
    val conv = mkConvFromElement[T]
    assertEquals(T(111,"test","test desc"),
      conv(
        CompositeE(List(
          ValueE("111"),
          ValueE("test"),
          ValueE("test desc"),
          NullE
        ))
      ))

    val conv1 = mkConvToElement[T]
    assertEquals(
      CompositeE(List(
        ValueE("112"),
        ValueE("test"),
        ValueE("test 2"),
        NullE)),
      conv1(T(112, "test", "test 2")))

    // simple nested case
    TypeConverters.register(conv)
    val convt1 = mkConvFromElement[T1]
    assertEquals(T1(115, T(111,"test","test dd",Some("hi")), List(157)),
      convt1(
        CompositeE(List(
          ValueE("115"),
          CompositeE(List(
            ValueE("111"),
            ValueE("test"),
            ValueE("test dd"),
            ValueE("hi"))),
          ArrayE(List(
            ValueE("157")
          ))
        ))
      ))

    TypeConverters.register(conv1)
    val convt11 = mkConvToElement[T1]
    assertEquals(
      CompositeE(List(
        ValueE("116"),
        CompositeE(List(
          ValueE("111"),
          ValueE("test"),
          ValueE("test 3"),
          NullE)),
        ArrayE(List(
          ValueE("123"),
          ValueE("135")
        ))
      )),
      convt11(T1(116, T(111, "test", "test 3"), List(123,135))))

    // composite array case
    TypeConverters.register(convt1)
    val convat = mkArrayConvFromElement[T1]
    assertEquals(List(T1(115, T(111,"test","test dd",Some("hi")), List(157))),
      convat(
        ArrayE(List(
          CompositeE(List(
            ValueE("115"),
            CompositeE(List(
              ValueE("111"),
              ValueE("test"),
              ValueE("test dd"),
              ValueE("hi"))
            ),
            ArrayE(List(
              ValueE("157")
            ))
          ))
        ))
      ))

    TypeConverters.register(convt11)
    val convat1 = mkArrayConvToElement[T1]
    assertEquals(
      ArrayE(List(
        CompositeE(List(
          ValueE("115"),
          CompositeE(List(
            ValueE("111"),
            ValueE("test"),
            ValueE("test dd"),
            ValueE("hi"))),
          ArrayE(List(
            ValueE("157")
          ))
        ))
      )),
      convat1(List(T1(115, T(111,"test","test dd",Some("hi")), List(157)))))
  }
}

object TypeConverterTest {
  // !!!NOTE: should use outer (in object) classes, instead of inner (in trait/class) classes
  case class T(id: Long, name: String, desc: String, opt: Option[String] = None)
  case class T1(id: Long, t: T, childIds: List[Long])
}
