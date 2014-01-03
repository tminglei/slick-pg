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
    assertEquals(input, PGObjectTokenizer.reverse(expected))

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
    assertEquals(input1, PGObjectTokenizer.reverse(expected1))

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
    assertEquals(input2, PGObjectTokenizer.reverse(expected2))

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
    assertEquals(input3, PGObjectTokenizer.reverse(expected3))
  }

  @Test
  def testSimpleNestedComposite(): Unit = {
    val input = """(115,"(111,test,""test dd"",hi)",{157})"""
    val input_v = """(115,"(111,test,""test dd"",hi)","{157}")"""
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
    assertEquals(input_v, PGObjectTokenizer.reverse(expected))

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
    assertEquals(input1, PGObjectTokenizer.reverse(expected1))

    ///
    val input2 = """(115,,"{157}")"""
    val expected2 =
      CompositeE(List(
        ValueE("115"),
        NullE,
        ArrayE(List(
          ValueE("157")
        ))
      ))

    assertEquals(expected2, PGObjectTokenizer.tokenize(input2))
    assertEquals(input2, PGObjectTokenizer.reverse(expected2))
  }

  @Test
  def testArrayNestedComposite(): Unit = {
    val input = """{"(115,\"(111,test,\"\"test dd\"\",hi)\",\"{157,111}\")"}"""
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
            ValueE("157"),
            ValueE("111")
          ))
        ))
      ))

    assertEquals(expected, PGObjectTokenizer.tokenize(input))
    assertEquals(input, PGObjectTokenizer.reverse(expected))
  }

  @Test
  def testNestedArrayComposite(): Unit = {
    val input = """(115,"{""(111,test,\\""test desc\\"",hi)""}",{157})"""
    val input_v = """(115,"{""(111,test,\\""test desc\\"",hi)""}","{157}")"""
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
    assertEquals(input_v, PGObjectTokenizer.reverse(expected))
  }

  @Test
  def testComplexNestedComposite(): Unit = {
    val input = """(115,"{""(111,test,\\""(103,ttt)\\"",hi)""}",{157})"""
    val input_v = """(115,"{""(111,test,\\""(103,ttt)\\"",hi)""}","{157}")"""
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
    assertEquals(input_v, PGObjectTokenizer.reverse(expected))

    ///
    val input1 = """(115,"{""(111,test,\\""(103,ttt,\\""\\""{\\""\\""\\""\\""(131,\\\\\\\\\\\\\\\\\\""\\""\\""\\""tl k\\\\\\\\\\\\\\\\\\""\\""\\""\\"")\\""\\""\\""\\""}\\""\\"")\\"",hi)""}",{157})"""
    val input1v = """(115,"{""(111,test,\\""(103,ttt,\\\\\\""{\\\\\\\\\\\\\\""(131,\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\""tl k\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"")\\\\\\\\\\\\\\""}\\\\\\"")\\"",hi)""}","{157}")"""
    val expected1 = //row(115,array[row(111,'test',row(103,'ttt',array[row(131,'tl k')]),'hi')],array[157]);
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
    assertEquals(input1v, PGObjectTokenizer.reverse(expected1))

    ///
    val input2 = """(t4,"(t3,""{""\""(t2,\\\\""\""{\\\\""\""\\\\""\""(t1,\\\\\\\\\\\\\\\\\\\\""\""\\\\""\""{99,13}\\\\\\\\\\\\\\\\\\\\""\""\\\\""\"")\\\\""\""\\\\""\""}\\\\""\"")""\""}"")")"""
    val input2v = """(t4,"(t3,""{\\""(t2,\\\\\\""{\\\\\\\\\\\\\\""(t1,\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\""{99,13}\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"")\\\\\\\\\\\\\\""}\\\\\\"")\\""}"")")"""
    val expected2 = //row('t4', row('t3', array[row('t2', array[row('t1', array[99,13])])]));
      CompositeE(List(
        ValueE("t4"),
        CompositeE(List(
          ValueE("t3"),
          ArrayE(List(
            CompositeE(List(
              ValueE("t2"),
              ArrayE(List(
                CompositeE(List(
                  ValueE("t1"),
                  ArrayE(List(
                    ValueE("99"),
                    ValueE("13")
                  ))
                ))
              ))
            ))
          ))
        ))
      ))

    assertEquals(expected2, PGObjectTokenizer.tokenize(input2))
    assertEquals(input2v, PGObjectTokenizer.reverse(expected2))

    ///
    val input3 = """{"(301,\"{\"\"(201,\\\\\"\"{\\\\\"\"\\\\\"\"(101,\\\\\\\\\\\\\\\\\\\\\"\"\\\\\"\"{99,13}\\\\\\\\\\\\\\\\\\\\\"\"\\\\\"\")\\\\\"\"\\\\\"\"}\\\\\"\")\"\"}\")"}"""
    val input3v = """{"(301,\"{\"\"(201,\\\\\"\"{\\\\\\\\\\\\\"\"(101,\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"\"{99,13}\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"\")\\\\\\\\\\\\\"\"}\\\\\"\")\"\"}\")"}"""
    val expected3 = //array[row(301, array[row(201, array[row(101, array[99,13])])])]
      ArrayE(List(
        CompositeE(List(
          ValueE("301"),
          ArrayE(List(
            CompositeE(List(
              ValueE("201"),
              ArrayE(List(
                CompositeE(List(
                  ValueE("101"),
                  ArrayE(List(
                    ValueE("99"),
                    ValueE("13")
                  ))
                ))
              ))
            ))
          ))
        ))
      ))

    assertEquals(expected3, PGObjectTokenizer.tokenize(input3))
    assertEquals(input3v, PGObjectTokenizer.reverse(expected3))
  }
}
