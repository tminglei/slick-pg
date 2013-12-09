package com.github.tminglei.slickpg
package utils

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader
import scala.slick.SlickException

class PGObjectTokenizer extends RegexParsers {

  // pg tokens, should be used internally only
  object PGTokens {
    sealed trait Token
    case class Comma()                          extends Token

    case class ArrOpen(marker: String,l: Int)   extends Token
    case class RecOpen(marker: String,l: Int)   extends Token
    case class ArrClose(marker: String,l: Int)  extends Token
    case class RecClose(marker: String,l: Int)  extends Token
    case class Escape(l:Int, escape: String)    extends Token
    case class Marker(m : String, l: Int)       extends Token
    case class SingleQuote()                    extends Token

    trait ValueToken extends Token {
      def value: String
    }
    case class Chunk(value : String)            extends ValueToken
    case class Quote(value: String)             extends ValueToken

    trait CompositeToken extends Token {
      def value: List[Token]
    }
    case class CTArray(value: List[Token])      extends CompositeToken
    case class CTRecord(value: List[Token])     extends CompositeToken
    case class CTString(value: List[Token])     extends CompositeToken
  }

  ////////////////////////////////////
  import PGTokens._
  import PGObjectTokenizer.PGElements._

  object PGTokenReducer {

    def compose(input : CompositeToken) : Element =  {

      @tailrec
      def mergeString(list : List[Token], tally: String = "") : String = {
        if(list.isEmpty) tally
        else
          list.head match {
            case Chunk(v) => mergeString(list.tail, tally + v)
            case Quote(v) => mergeString(list.tail, tally + v)
            case Escape(_,v) => mergeString(list.tail, tally + v)
            case Comma() => mergeString(list.tail, tally)
          }
      }

      // postgress should never return any ws between chunks and commas. for example : (1, ,2, )
      // This case class would handle that :
      // case Chunk(v) if v.trim.isEmpty => null
      //----------------------------------------------
      // This (1 ," lalal"   ,) on the other hand would be a seperate matter.

      def mergeComposite(composite :CompositeToken, level: Int = 0) : Element = {
        val elements =
          composite.value.collect {
            case v: CTArray   => mergeComposite(v)
            case v: CTRecord  => mergeComposite(v)
            case CTString(v)  => ValueE(mergeString(v.slice(1,v.length-1)))
            case Chunk(v) => ValueE(v)
            case null => null
          }

        composite match {
          case CTArray(_) => ArrayE(elements)
          case CTRecord(_) => CompositeE(elements)
          case token => throw new IllegalArgumentException(s"unsupported token $token")
        }
      }

      //--
      mergeComposite(input)
    }

    def reduce(tokens : List[Token]): CompositeToken = {

      def isOpen(token: Token) = { token match {
        case ArrOpen(_,_) => true
        case RecOpen(_,_) => true
        case SingleQuote() => true
        case _ => false
      } }

      def innerClose(x : Token, xs: List[Token],dpth: Int) = {
        val ret = close(x,x,xs,x::List(),dpth +1)
        (ret._1.asInstanceOf[CompositeToken], ret._2)
      }

      def nullCheck(lastToken: Token, targetList : List[Token]  ) : List[Token] = {
        lastToken match {
          case Comma() => targetList :+ null
          case _ => targetList
      } }

      @tailrec
      def close(borderToken: Token, lastToken: Token, source: List[Token], target : List[Token],depth: Int = 0,consumeCount: Int =1): (CompositeToken,Int) = {
        source match {
          case List() => throw new Exception("reduction should never hit recursive empty list base case.")
          case x :: xs =>
            x match {
              // CLOSING CASES
              case ArrClose(cm,cl) => borderToken match {
                case ArrOpen(sm,sl) if cm == sm && sl==cl => ( CTArray(nullCheck(lastToken,target) :+ x),consumeCount + 1)
                case _ => throw new Exception (s"open and close tags don't match : $borderToken - $x")
              }
              case RecClose(cm,cl) => borderToken match {
                case RecOpen(sm,sl) if cm == sm && sl == cl => (CTRecord(nullCheck(lastToken,target) :+ x),consumeCount + 1)
                case _ => throw new Exception (s"open and close tags don't match:  : $borderToken - $x")
              }
              case SingleQuote() if borderToken.isInstanceOf[SingleQuote] => (CTString(target :+ x ),consumeCount +1)
              // the else porting of this should be caught by the isOpen case below
              // OPENING CASES -> The results of these are siblings.
              case xx if isOpen(x) => {
                val (sibling, consumed) = innerClose(x,xs,depth+1)
                val new_source =  source.splitAt(consumed)._2
                close(borderToken,x,new_source,target :+ sibling,consumeCount = consumeCount + consumed)
              }
              case Comma() => close(borderToken,x,xs,nullCheck(lastToken,target) :+ x, consumeCount = consumeCount + 1)
              case _ => close(borderToken, x, xs, target :+ x, consumeCount = consumeCount +1)
            }
        }
      }

      //--
      tokens match {
        case x :: xs if xs != List() =>
          val ret =
            x match {
              case xx if isOpen(x)  => close(x,x, xs, x :: List() )
              case _ => throw new Exception("open must always deal with an open token.")
            }
          if(ret._2 != tokens.size)
            throw new Exception("reduction step did not cover all tokens.")
          ret._1
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////////////
  import PGTokenReducer._
  
  override def skipWhitespace = false
  
  var level = -1
  var levelMarker = new mutable.Stack[String]()

  def markerRe = "[\\\\|\"]+".r

  def open: Parser[Token] = opt(markerRe) ~ (elem('{') | elem('(')) ^^ { case(x ~ y) =>
    val marker_ = x.getOrElse("")
    levelMarker.push(marker_)
    val r = y match {
      case '{' => ArrOpen(marker_,level)
      case '(' => RecOpen(marker_,level)
    } ; level += 1; r }

  def close: Parser[Token] = (elem('}') | elem(')')) ~ opt(markerRe) ^^ { case (x~ y) =>
    level -= 1 ; levelMarker.pop()
    x match {
      case '}' => ArrClose(y.getOrElse(""),level)
      case ')' => RecClose(y.getOrElse(""),level)
    } }

  def escape = """\\+[^"\\]""".r ^^ {x => Escape(level,"\\" + x.last)}

  def marker =  markerRe ^^ { x=>
    val pow = scala.math.pow(2,level).toInt
    if(x.length % pow == 0) {
      x.length / pow match {
        case 1 => SingleQuote()
        case _ => Quote("\"" * (x.length/pow))
      }
    }
    else {
      Marker(x,level)
    }
  }

  def comma = elem(',') ^^ { x=> Comma() }
  def chunk = """[^}){(\\,"]+""".r ^^ { Chunk}
  def tokens = open | close | escape | marker | comma | chunk

  def tokenize = rep(tokens)  ^^ { t=> compose(reduce(t)) }

  //--
  def process(input : String) = {
    parseAll(tokenize, new CharSequenceReader(input))
  }
}

object PGObjectTokenizer extends PGObjectTokenizer {

  object PGElements {
    sealed trait Element
    case class ValueE(value: String) extends Element
    case class ArrayE(elements: List[Element]) extends Element
    case class CompositeE(members: List[Element]) extends Element
  }

  def apply(input : String): PGElements.Element = {
    new PGObjectTokenizer().process(input) match {
      case Success(result, _) => result
      case failure: NoSuccess => throw new SlickException(failure.msg)
    }
  }
}