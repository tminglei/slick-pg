package com.github.tminglei.slickpg
package utils

import org.junit._
import org.junit.Assert._
import PGObjectTokenizer.PGElements._

class PGObjectTokenizerTest {

  @Test
  def testSimpleComposite(): Unit = {
    val input = """(111,test,"test desc",hi)"""
    val expected =
      CompositeE(List(
        ValueE("111"),
        ValueE("test"),
        ValueE("test desc"),
        ValueE("hi")
      ))

    assertEquals(expected, PGObjectTokenizer.tokenize(input))

    ///
    val input1 = """(111,test,"test desc",)"""
    val expected1 =
      CompositeE(List(
        ValueE("111"),
        ValueE("test"),
        ValueE("test desc"),
        NullE
      ))

    assertEquals(expected1, PGObjectTokenizer.tokenize(input1))

    ///
    val input2 = """(111,test,,)"""
    val expected2 =
      CompositeE(List(
        ValueE("111"),
        ValueE("test"),
        NullE,
        NullE
      ))
    assertEquals(expected2, PGObjectTokenizer.tokenize(input2))

    //
    val input3 = """(111,,"test desc",)"""
    val expected3 =
      CompositeE(List(
        ValueE("111"),
        NullE,
        ValueE("test desc"),
        NullE
      ))
    assertEquals(expected3, PGObjectTokenizer.tokenize(input3))
  }

  @Test
  def testSimpleNestedComposite(): Unit = {
    val input = """(115,"(111,test,""test dd"",hi)",{157})"""
    val expected =
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

    assertEquals(expected, PGObjectTokenizer.tokenize(input))

    val input01 = """(115,"(111,""test"",""test dd"",hi)",{157})"""
    assertEquals(expected, PGObjectTokenizer.tokenize(input01))

    ///
    val input1 = """(115,"(111,test,""test dd"",hi)",)"""
    val expected1 =
      CompositeE(List(
        ValueE("115"),
        CompositeE(List(
          ValueE("111"),
          ValueE("test"),
          ValueE("test dd"),
          ValueE("hi"))),
        NullE
      ))

    assertEquals(expected1, PGObjectTokenizer.tokenize(input1))

    ///
    val input2 = """(115,,{157})"""
    val expected2 =
      CompositeE(List(
        ValueE("115"),
        NullE,
        ArrayE(List(
          ValueE("157")
        ))
      ))

    assertEquals(expected2, PGObjectTokenizer.tokenize(input2))
  }

  @Test
  def testArrayNestedComposite(): Unit = {
    val input = """"{"(115,\"(111,test,\"\"test dd\"\",hi)\",{157})"}""""
    val expected =
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
      ))

    assertEquals(expected, PGObjectTokenizer.tokenize(input))
  }

  @Test
  def testNestedArrayComposite(): Unit = {
    val input = """"(115,"{""(111,test,\\""test desc\\"",hi)""}",{157})""""
    val expected =
      CompositeE(List(
        ValueE("115"),
        ArrayE(List(
          CompositeE(List(
            ValueE("111"),
            ValueE("test"),
            ValueE("test desc"),
            ValueE("hi")
          ))
        )),
        ArrayE(List(
          ValueE("157")
        ))
      ))

    assertEquals(expected, PGObjectTokenizer.tokenize(input))
  }

  @Test
  def testComplexNestedComposite(): Unit = {
    val input = """"(115,"{""(111,test,\\""(103,ttt)\\"",hi)""}",{157})""""
    val expected =
      CompositeE(List(
        ValueE("115"),
        ArrayE(List(
          CompositeE(List(
            ValueE("111"),
            ValueE("test"),
            CompositeE(List(
              ValueE("103"),
              ValueE("ttt")
            )),
            ValueE("hi")
          ))
        )),
        ArrayE(List(
          ValueE("157")
        ))
      ))

    assertEquals(expected, PGObjectTokenizer.tokenize(input))

    ///
    val input1 = """""(115,"{""(111,test,\\""(103,ttt,\\""\\""{\\""\\""\\""\\""(131,\\\\\\\\\\\\\\\\\\""\\""\\""\\""tl k\\\\\\\\\\\\\\\\\\""\\""\\""\\"")\\""\\""\\""\\""}\\""\\"")\\"",hi)""}",{157})"""""
    val expected1 =
      CompositeE(List(
        ValueE("115"),
        ArrayE(List(
          CompositeE(List(
            ValueE("111"),
            ValueE("test"),
            CompositeE(List(
              ValueE("103"),
              ValueE("ttt"),
              ArrayE(List(
                CompositeE(List(
                  ValueE("131"),
                  ValueE("tl k")
                ))
              ))
            )),
            ValueE("hi")
          ))
        )),
        ArrayE(List(
          ValueE("157")
        ))
      ))

    assertEquals(expected1, PGObjectTokenizer.tokenize(input1))
  }
}
