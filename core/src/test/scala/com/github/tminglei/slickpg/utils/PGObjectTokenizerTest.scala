package com.github.tminglei.slickpg
package utils

import org.junit._
import org.junit.Assert._
import PGObjectTokenizer.PGElements._

class PGObjectTokenizerTest {

  @Test
  def testSimpleComposite(): Unit = {
    val input = """(111,test,"test desc","2010-01-01 14:30")"""
    val expected =
      CompositeE(List(
        ValueE("111"),
        ValueE("test"),
        ValueE("test desc"),
        ValueE("2010-01-01 14:30")
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
    val input = """(115,"(111,test,""test dd"",""2010-01-01 14:30"")",{157})"""
    val input_v = """(115,"(111,test,""test dd"",""2010-01-01 14:30"")","{157}")"""
    val expected =
      CompositeE(List(
        ValueE("115"),
        CompositeE(List(
          ValueE("111"),
          ValueE("test"),
          ValueE("test dd"),
          ValueE("2010-01-01 14:30"))),
        ArrayE(List(
          ValueE("157")
        ))
      ))

    assertEquals(expected, PGObjectTokenizer.tokenize(input))
    assertEquals(input_v, PGObjectTokenizer.reverse(expected))

    val input01 = """(115,"(111,""test"",""test dd"",""2010-01-01 14:30"")",{157})"""
    assertEquals(expected, PGObjectTokenizer.tokenize(input01))

    ///
    val input1 = """(115,"(111,test,""test dd"",""2010-01-01 14:30"")",)"""
    val expected1 =
      CompositeE(List(
        ValueE("115"),
        CompositeE(List(
          ValueE("111"),
          ValueE("test"),
          ValueE("test dd"),
          ValueE("2010-01-01 14:30"))),
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
    val input = """(115,"{""(111,test,\\""test desc\\"",\\""2010-01-01 14:30\\"")""}",{157})"""
    val input_v = """(115,"{""(111,test,\\""test desc\\"",\\""2010-01-01 14:30\\"")""}","{157}")"""
    val expected =
      CompositeE(List(
        ValueE("115"),
        ArrayE(List(
          CompositeE(List(
            ValueE("111"),
            ValueE("test"),
            ValueE("test desc"),
            ValueE("2010-01-01 14:30")
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
    val input = """(115,"{""(111,test,\\""(103,ttt)\\"",\\""2010-01-01 14:30\\"")""}",{157})"""
    val input_v = """(115,"{""(111,test,\\""(103,ttt)\\"",\\""2010-01-01 14:30\\"")""}","{157}")"""
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
            ValueE("2010-01-01 14:30")
          ))
        )),
        ArrayE(List(
          ValueE("157")
        ))
      ))

    assertEquals(expected, PGObjectTokenizer.tokenize(input))
    assertEquals(input_v, PGObjectTokenizer.reverse(expected))

    ///
    val input1 = """(&#40;t4,"(""&#92;t3"",""{""\""(&#123;t2,\\\\""\""{\\\\""\""\\\\""\""(t1,\\\\\\\\\\\\\\\\\\\\""\""\\\\""\""{99,13}\\\\\\\\\\\\\\\\\\\\""\""\\\\""\"")\\\\""\""\\\\""\""}\\\\""\"")""\""}"")")"""
    val input1v = """("&#40;t4","(""&#92;t3"",""{\\""(&#123;t2,\\\\\\""{\\\\\\\\\\\\\\""(t1,\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\""{99,13}\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"")\\\\\\\\\\\\\\""}\\\\\\"")\\""}"")")"""
    val expected1 = //row('(t4', row('\t3', array[row('{t2', array[row('t1', array[99,13])])]));
      CompositeE(List(
        ValueE("(t4"),
        CompositeE(List(
          ValueE("\\t3"),
          ArrayE(List(
            CompositeE(List(
              ValueE("{t2"),
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

    assertEquals(expected1, PGObjectTokenizer.tokenize(input1))
    assertEquals(input1v, PGObjectTokenizer.reverse(expected1))

    ///
    val input2 = """{"(\"t3&#34;s\",\"{\"\"(t2's,\\\\\"\"{\\\\\"\"\\\\\"\"(\\\\\\\\\\\\\\\\\\\\\"\"\\\\\"\"t1 s\\\\\\\\\\\\\\\\\\\\\"\"\\\\\"\",)\\\\\"\"\\\\\"\"}\\\\\"\")\"\"}\")"}"""
    val input2v = """{"(\"t3&#34;s\",\"{\"\"(t2''s,\\\\\"\"{\\\\\\\\\\\\\"\"(\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"\"t1 s\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"\",)\\\\\\\\\\\\\"\"}\\\\\"\")\"\"}\")"}"""
    val expected2 = //array[row('t3"s', array[row('t2''s', array[row('t1,s', null)])])];
      ArrayE(List(
        CompositeE(List(
          ValueE("t3\"s"),
          ArrayE(List(
            CompositeE(List(
              ValueE("t2's"),
              ArrayE(List(
                CompositeE(List(
                  ValueE("t1 s"),
                  NullE
                ))
              ))
            ))
          ))
        ))
      ))

    assertEquals(expected2, PGObjectTokenizer.tokenize(input2))
    assertEquals(input2v, PGObjectTokenizer.reverse(expected2))

    ///
    val input3 = """{"(t3,\"(t2,\"\"(t1,\"\"\"\"{\"\"\"\"\"\"\"\"(t,\\\\\\\\\\\\\\\\\"\"\"\"\"\"\"\"[2010-01-01 14:30, 2010-01-01 15:30)\\\\\\\\\\\\\\\\\"\"\"\"\"\"\"\")\"\"\"\"\"\"\"\"}\"\"\"\")\"\")\")"}"""
    val input3v = """{"(t3,\"(t2,\"\"(t1,\\\\\"\"{\\\\\\\\\\\\\"\"(t,\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"\"[2010-01-01 14:30, 2010-01-01 15:30)\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"\")\\\\\\\\\\\\\"\"}\\\\\"\")\"\")\")"}"""
    val expected3 = //array[row('t3', row('t2', row('t1', array[row('t', '[2010-01-01 14:30, 2010-01-01 15:30)')])))]
      ArrayE(List(
        CompositeE(List(
          ValueE("t3"),
          CompositeE(List(
            ValueE("t2"),
            CompositeE(List(
              ValueE("t1"),
              ArrayE(List(
                CompositeE(List(
                  ValueE("t"),
                  ValueE("[2010-01-01 14:30, 2010-01-01 15:30)")
                ))
              ))
            ))
          ))
        ))
      ))

    assertEquals(expected3, PGObjectTokenizer.tokenize(input3))
    assertEquals(input3v, PGObjectTokenizer.reverse(expected3))
  }

  @Test
  def testPatternsInUsed(): Unit = {
    val tokenizer = new PGObjectTokenizer

    import tokenizer.PGTokenReverser.{MARK_LETTERS, RANGE_STRING}

    assertTrue(MARK_LETTERS findFirstIn "xxx,tt" isDefined)
    assertTrue(MARK_LETTERS findFirstIn "\\ttt" isDefined)
    assertTrue(MARK_LETTERS findFirstIn "tt st" isDefined)
    assertTrue(MARK_LETTERS findFirstIn "(ttt" isDefined)
    assertTrue(MARK_LETTERS findFirstIn "ttt)" isDefined)

    assertTrue(RANGE_STRING findFirstIn "[111,234)" isDefined)
    assertTrue(RANGE_STRING findFirstIn "[2013-11-4, 2014-1-1)" isDefined)
    assertTrue(RANGE_STRING findFirstIn "[\"2013-11-4\", \"2014-1-1\")" isDefined)

    import tokenizer.PGTokenReducer.{DATE, TIMESTAMP, INTEGER, FLOAT}

    assertTrue(INTEGER findFirstIn "111" isDefined)
    assertTrue(INTEGER findFirstIn " 111" isEmpty)
    assertTrue(FLOAT findFirstIn "103.22" isDefined)
    assertTrue(FLOAT findFirstIn "te 134.1" isEmpty)
    assertTrue(DATE findFirstIn "2013-11-1" isDefined)
    assertTrue(DATE findFirstIn "tt 2013-11-01" isEmpty)
    assertTrue(TIMESTAMP findFirstIn "2010-01-1 14:30" isDefined)
    assertTrue(TIMESTAMP findFirstIn "2010-01-01T14:30:00" isDefined)
    assertTrue(TIMESTAMP findFirstIn "tt 2010-01-01 14:30" isEmpty)

    assertTrue(
      "111" match {
        case INTEGER() => true
        case _ => false
      })
    assertTrue(
      "103.22" match {
        case FLOAT(_) => true
        case _ => false
      })
    assertTrue(
      "2013-11-01" match {
        case DATE() => true
        case _ => false
      })
    assertTrue(
      "2010-01-01 14:30" match {
        case TIMESTAMP(_,_) => true
        case _ => false
      })
    assertTrue(
      ("2010-01-01 14:30", "2010-01-01 15:30") match {
        case (TIMESTAMP(_,_), TIMESTAMP(_,_)) => true
        case _  => false
      })
  }
}
