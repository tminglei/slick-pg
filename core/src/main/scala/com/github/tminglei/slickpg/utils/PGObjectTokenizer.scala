import scala.collection.mutable
import scala.util.parsing.combinator.RegexParsers

sealed trait PGTokens
{
  trait Token
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

sealed trait PGElements {
  sealed trait Element
  case class ValueE(value: String) extends Element
  case class ArrayE(elements: List[Element]) extends Element
  case class CompositeE(members: List[Element]) extends Element
}

sealed trait PGTokenReducer extends PGTokens with PGElements {
  def compose(input : CompositeToken) : Element =  {
    def mergeString(list : List[Token]) : String = {
      if(list.isEmpty) ""
      else
        list.head match {
          case Chunk(v) => v + mergeString(list.tail)
          case Quote(v) => v + mergeString(list.tail)
          case Escape(_,v) => v + mergeString(list.tail)
          case Comma() => mergeString(list.tail)
        }
    }
    def mergeComposite(composite :CompositeToken, level: Int = 0) : Element = {
      val elements =
      composite.value.collect {
        case v: CTArray   => mergeComposite(v)
        case v: CTRecord  => mergeComposite(v)
        case CTString(v)  => ValueE(mergeString(v.slice(1,v.length-1)))
        case Chunk(v) => ValueE(v)
      }

      composite match {
        case CTArray(_) => ArrayE(elements)
        case CTRecord(_) => CompositeE(elements)
        case _ => throw new Exception("merge composite should only be called for composites.")
      }

    }
    mergeComposite(input)
  }

  def reduce(tokens : List[Token]): CompositeToken = {
    def isOpen(token: Token) = { token match {
      case ArrOpen(_,_) => true
      case RecOpen(_,_) => true
      case SingleQuote() => true
      case _ => false
    } }

    def close(borderToken: Token, source: List[Token], target : List[Token],depth: Int = 0,consumeCount: Int =1): (CompositeToken,Int) = {
      source match {
        case List() => throw new Exception("reduction should never hit recursive empty list base case.")
        case x :: xs =>
          x match {
            // CLOSING CASES
            case ArrClose(cm,cl) => borderToken match {
              case ArrOpen(sm,sl) if cm == sm && sl==cl => (CTArray(target :+ x),consumeCount + 1)
              case _ => throw new Exception (s"open and close tags don't match : $borderToken - $x")
            }
            case RecClose(cm,cl) => borderToken match {
              case RecOpen(sm,sl) if cm == sm && sl == cl => (CTRecord(target :+ x),consumeCount + 1)
              case _ => throw new Exception (s"open and close tags don't match:  : $borderToken - $x")
            }
            case SingleQuote() if borderToken.isInstanceOf[SingleQuote] => (CTString(target :+ x ),consumeCount +1)
            // the else porting of this should be caught by the isOpen case below
            // OPENING CASES -> The results of these are siblings.
            case xx if isOpen(x) => {
              val (sibling, consumed) = { val ret = close(x,xs,x::List(),depth +1); (ret._1.asInstanceOf[CompositeToken], ret._2) }
              val new_source =  source.splitAt(consumed)._2
              close(borderToken,new_source,target :+ sibling,consumeCount = consumeCount + consumed)
            }
            case _ => close(borderToken, xs, target :+ x, consumeCount = consumeCount +1)
          }
      }
    }

    tokens match {
      case x :: xs if xs != List() =>
        val ret =
          x match {
            case xx if isOpen(x)  => close(x, xs, x :: List() )
            case _ => throw new Exception("open must always deal with an open token.")
          }
        if(ret._2 != tokens.size)
          throw new Exception("reduction step did not cover all tokens.")
        ret._1
    }
  }
}

object PGObjectTokenizer extends RegexParsers with PGTokens with PGTokenReducer {
  var level = -1
  var levelMarker = new mutable.Stack[String]()

  def markerRe = "[\\\\|\"]+".r

  def open: Parser[Token] = opt(markerRe) ~ (elem('{') | elem('(')) ^^ { case(x ~ y) =>
    val marker_ = x.getOrElse("")
    levelMarker.push(marker_)
    val r = y match {
      case '{' => RecOpen(marker_,level)
      case '(' => ArrOpen(marker_,level)
    } ; level += 1; r }

  def close: Parser[Token] = (elem('}') | elem(')')) ~ opt(markerRe) ^^ { case (x~ y) =>
    level -= 1 ; levelMarker.pop()
    x match {
      case '}' => RecClose(y.getOrElse(""),level)
      case ')' => ArrClose(y.getOrElse(""),level)
    } }

  def escape = """\\+[^"\\]""".r ^^ {x => Escape(level,"\\" + x.last)}

  def marker =  markerRe ^^ { x=> {
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
  } }

  def comma = elem(',') ^^ { x=> Comma() }
  def chunk = """[^}){(\\,"]+""".r ^^ { Chunk}
  def tokens = open | close | escape | marker | comma | chunk

  def tokenise = rep(tokens)  ^^ { tl => compose(reduce(tl)) }



  def apply(input : String) = {
    level = -1
    levelMarker.clear()
    println(input)
    parseAll(tokenise,new scala.util.parsing.input.CharArrayReader(input.toCharArray))
  }
}