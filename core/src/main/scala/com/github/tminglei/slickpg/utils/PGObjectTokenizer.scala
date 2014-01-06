package com.github.tminglei.slickpg
package utils

import scala.annotation.tailrec
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader
import scala.slick.SlickException

class PGObjectTokenizer extends RegexParsers {

  // pg tokens, should be used internally only
  object PGTokens {
    sealed trait Token
    case object Comma                           extends Token

    trait BorderToken extends Token
    case class BraceOpen(marker: String, l: Int)  extends BorderToken
    case class BraceClose(marker: String, l: Int) extends BorderToken
    case class ParenOpen(marker: String, l: Int)  extends BorderToken
    case class ParenClose(marker: String, l: Int) extends BorderToken
    case class BracketOpen(marker: String, l: Int)  extends BorderToken
    case class BracketClose(marker: String, l: Int) extends BorderToken
    case class Marker(marker: String, l: Int)   extends BorderToken
    case object SingleQuote                     extends BorderToken

    trait ValueToken extends Token {
      def value: String
    }
    case class Chunk(value: String)             extends ValueToken
    case class Quote(value: String)             extends ValueToken
    case class Escape(value: String, l:Int)     extends ValueToken

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

    def compose(input: CompositeToken): Element =  {

      @tailrec
      def mergeString(list: List[Token], tally: String = ""): String = {
        if (list.isEmpty) tally
        else
          list.head match {
            case Chunk(v) => mergeString(list.tail, tally + v)
            case Quote(v) => mergeString(list.tail, tally + v)
            case Escape(v, _) => mergeString(list.tail, tally + v)
            case Comma  => mergeString(list.tail, tally)
            case token => throw new IllegalArgumentException(s"unsupported token $token")
          }
      }
      
      def unescape(str: String): String =
        if (str.contains("&#")) {
          str
            .replaceAllLiterally("&#34;", "\"")
            .replaceAllLiterally("&#92;", "\\")
            .replaceAllLiterally("&#40;", "(")
            .replaceAllLiterally("&#41;", ")")
            .replaceAllLiterally("&#123;", "{")
            .replaceAllLiterally("&#125;", "}")
            .replaceAllLiterally("&#91;", "[")
            .replaceAllLiterally("&#93;", "]")
            .replaceAllLiterally("&#44;", ",")
        } else str

      // postgres should never return any ws between chunks and commas. for example: (1, ,2, )
      // This case class would handle that:
      // case Chunk(v) if v.trim.isEmpty => null
      //----------------------------------------------
      // This (1 ," lalal"   ,) on the other hand would be a separate matter.
      def mergeComposite(composite: CompositeToken, level: Int = 0): Element = {
        val elements =
          composite.value.collect {
            case v: CTArray   => mergeComposite(v)
            case v: CTRecord  => mergeComposite(v)
            case CTString(v)  => ValueE(unescape(mergeString(v.filterNot(_.isInstanceOf[BorderToken]))))
            case Chunk(v) => ValueE(unescape(v))
            case null => NullE
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

    ///
    val INTEGER = """^[-+]?[0-9]+$""".r
    val FLOAT = """^[-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?$""".r
    val DATE = """^\d{4}\-[01]?\d\-[0123]?\d$""".r
    val TIMESTAMP = """^\d{4}\-[01]?\d\-[0123]?\d([\stT][\d:\.]*)?([zZ]|[+\-][\d\d]:?[\d\d])?$""".r

    def reduce(tokens: List[Token]): CompositeToken = {

      def isRange(tokens: List[Token]): Boolean = {
        val members = tokens.filter(_.isInstanceOf[Chunk]).map(_.asInstanceOf[Chunk])
        if (members.length != 2) false
        else {
          val (l, r) = (members(0).value.trim, members(1).value.trim)
          (l, r) match {
            case (INTEGER(), INTEGER())   => true
            case (FLOAT(_), FLOAT(_))   => true
            case (DATE(), DATE())     => true
            case (TIMESTAMP(_,_),TIMESTAMP(_,_)) => true
            case _  => false
          }
        }
      }

      def rangeTokens(open: String, tokens: List[Token], close: String): List[Token] = {
        val members = tokens.filter(_.isInstanceOf[Chunk]).map(_.asInstanceOf[Chunk])
        if (members.length != 2) throw new IllegalArgumentException(s"WRONG range tokens: $members")
        else {
          Chunk(open) +: members(0) +: Chunk(",") +: members(1) +: Chunk(close) +: Nil
        }
      }

      def isOpen(token: Token) = { token match {
        case BraceOpen(_,_) => true
        case ParenOpen(_,_) => true
        case BracketOpen(_,_)=> true
        case SingleQuote  => true
        case _ => false
      } }

      def nullCheck(lastToken: Token, targetList: List[Token]): List[Token] = {
        lastToken match {
          case Comma => targetList :+ null
          case _  => targetList
        } }

      def innerClose(x: Token, xs: List[Token], depth: Int) =
        close(x, x, xs, x::List(), depth +1)

      @tailrec
      def close(borderToken: Token, lastToken: Token, source: List[Token], target: List[Token],
                depth: Int = 0, consumeCount: Int = 1): (CompositeToken, Int) = {
        source match {
          case List() => throw new Exception("reduction should never hit recursive empty list base case.")
          case x :: xs =>
            x match {
              // CLOSING CASES
              case BraceClose(cm, cl) => borderToken match {
                case BraceOpen(sm,sl) if cm == sm && sl == cl => (CTArray(nullCheck(lastToken, target) :+ x), consumeCount + 1)
                case _ => throw new Exception (s"open and close tags don't match: $borderToken - $x")
              }
              case ParenClose(cm, cl) => borderToken match {
                case ParenOpen(sm,sl) if cm == sm && sl == cl => {
                  if (isRange(target)) (CTString(Chunk("(") +: target :+ Chunk(")")), consumeCount +1)
                  else (CTRecord(nullCheck(lastToken, target) :+ x), consumeCount + 1)
                }
                case BracketOpen(sm, sl) if cm == sm && sl == cl && isRange(target) => (CTString(rangeTokens("[", target, ")")), consumeCount +1)
                case _ => throw new Exception (s"open and close tags don't match: $borderToken - $x")
              }
              case BracketClose(cm, cl) => borderToken match {
                case ParenOpen(sm, sl) if cm == sm && sl == cl  && isRange(target) => (CTString(rangeTokens("(", target, "]")), consumeCount +1)
                case BracketOpen(sm, sl) if cm == sm && sl == cl  && isRange(target) => (CTString(rangeTokens("[", target, "]")), consumeCount +1)
                case _ => throw new Exception (s"open and close tags don't match: $borderToken - $x")
              }
              case SingleQuote if borderToken == SingleQuote => (CTString(target :+ x), consumeCount +1)
              // the else porting of this should be caught by the isOpen case below
              // OPENING CASES -> The results of these are siblings.
              case xx if isOpen(x) => {
                val (sibling, consumed) = innerClose(x, xs, depth+1)
                val new_source =  source.splitAt(consumed)._2
                close(borderToken, x, new_source, target :+ sibling, consumeCount = consumeCount + consumed)
              }
              case Comma => close(borderToken, x, xs, nullCheck(lastToken, target) :+ x, consumeCount = consumeCount + 1)
              case _ => close(borderToken, x, xs, target :+ x, consumeCount = consumeCount +1)
            }
        }
      }

      //--
      tokens match {
        case x :: xs if xs != List() =>
          val ret =
            x match {
              case xx if isOpen(x) => close(x, x, xs, x :: List() )
              case _ => throw new Exception("open must always deal with an open token.")
            }
          if(ret._2 != tokens.size)
            throw new Exception("reduction step did not cover all tokens.")
          ret._1
      }
    }
  }

  object PGTokenReverser {
    val MARK_LETTERS = """,|"|\\|\s|\(|\)""".r
    val RANGE_STRING = """^[\[|\(]([^,]*),[ ]*([^,]*)[\)|\]]$""".r

    def reverse(elem: Element): String = {

      def markRequired(str: String): Boolean = MARK_LETTERS findFirstIn str isDefined
      def bypassEscape(str: String): Boolean = RANGE_STRING findFirstIn str isDefined

      def addEscaped(buf: StringBuilder, ch: Char, level: Int, dual: Boolean): Unit =
        ch match {
          case '\''  => buf append "''"
          case '"'   => buf append "&#34;"
          case '\\'  => buf append "&#92;"
          case '('   => buf append "&#40;"
          case ')'   => buf append "&#41;"
          case '{'   => buf append "&#123;"
          case '}'   => buf append "&#125;"
          case '['   => buf append "&#91;"
          case ']'   => buf append "&#93;"
          case ','   => buf append "&#44;"
          case _  =>  buf append ch
        }

      def addMark(buf: StringBuilder, level: Int, dual: Boolean): Unit =
        level match {
          case l if l < 0 => // do nothing
          case 0    =>  buf append "\""
          case 1    =>  buf append (if (dual) "\"\"" else "\\\"")
          case 2    =>  buf append (if (dual) "\\\\\"\"" else "\\\"\\\"")
          case l if l > 2 => {
            for(i <- 1 to (scala.math.pow(2, level).toInt - 4))
              buf append "\\"
            buf append (if (dual) "\\\\\"\"" else "\\\"\\\"")
          }
        }

      def doReverse(buf: StringBuilder, elem: Element, level: Int, dual: Boolean): Unit =
        elem match {
          case ArrayE(vList) => {
            addMark(buf, level, dual)
            buf append "{"
            var first = true
            for(v <- vList) {
              if (first) first = false else buf append ","
              doReverse(buf, v, level + 1, dual)
            }
            buf append "}"
            addMark(buf, level, dual)
          }
          case CompositeE(eList) => {
            addMark(buf, level, dual)
            buf append "("
            var first = true
            for(e <- eList) {
              if (first) first = false else buf append ","
              doReverse(buf, e, level + 1, dual)
            }
            buf append ")"
            addMark(buf, level, dual)
          }
          case ValueE(v) => {
            if (markRequired(v)) {
              addMark(buf, level, dual)
              if (bypassEscape(v)) buf append v
              else {
                for(ch <- v) addEscaped(buf, ch, level + 1, dual)
              }
              addMark(buf, level, dual)
            } else {
              for(ch <- v) addEscaped(buf, ch, level + 1, dual)
            }
          }
          case NullE  =>  // do nothing
        }

      //--
      val buf = new StringBuilder
      doReverse(buf, elem, -1, elem.isInstanceOf[CompositeE])
      buf.toString
    }
  }

  ////////////////////////////////////////////////////////////////////////////////
  import PGTokenReducer._

  override def skipWhitespace = false

  var level = -1

  def markerRe = "[\\\\|\"]+".r

  def open: Parser[Token] = opt(markerRe) ~ (elem('{') | elem('(') | elem('[')) ^^ { case(x ~ y) =>
    val marker_ = x.getOrElse("")
    val r = y match {
      case '{' => BraceOpen(marker_,level)
      case '(' => ParenOpen(marker_,level)
      case '[' => BracketOpen(marker_,level)
    } ; level += 1; r }

  def close: Parser[Token] = (elem('}') | elem(')') | elem(']')) ~ opt(markerRe) ^^ { case (x~ y) =>
    level -= 1
    val marker_ = y.getOrElse("")
    x match {
      case '}' => BraceClose(marker_,level)
      case ')' => ParenClose(marker_,level)
      case ']' => BracketClose(marker_,level)
    } }

  def escape = """\\+[^"\\]""".r ^^ {x => Escape("\\" + x.last, level)}

  def marker = markerRe ^^ { x=>
    val pow = scala.math.pow(2,level).toInt
    if(x.length % pow == 0) {
      x.length / pow match {
        case 1 => SingleQuote
        case _ => Quote("\"" * (x.length/pow -1))
      }
    }
    else {
      Marker(x,level)
    }
  }

  def comma = elem(',') ^^ { x=> Comma }
  def chunk = """[^}){(\\,"]+""".r ^^ { Chunk}
  def tokens = open | close | escape | marker | comma | chunk

  def tokenParser = rep(tokens)  ^^ { t=> compose(reduce(t)) }

  //--
  def tokenize(input: String) =
    parseAll(tokenParser, new CharSequenceReader(input)) match {
      case Success(result, _) => result
      case failure: NoSuccess => throw new SlickException(failure.msg)
    }

  def reverse(elem: Element) = PGTokenReverser.reverse(elem)
}

object PGObjectTokenizer {

  object PGElements {
    sealed trait Element
    case class ValueE(value: String) extends Element
    case object NullE extends Element
    case class ArrayE(elements: List[Element]) extends Element
    case class CompositeE(members: List[Element]) extends Element
  }

  def tokenize(input: String): PGElements.Element = {
    new PGObjectTokenizer().tokenize(input)
  }

  def reverse(elem: PGElements.Element): String = {
    new PGObjectTokenizer().reverse(elem)
  }
}
