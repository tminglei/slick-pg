package com.github.tminglei.slickpg
package utils

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader
import scala.collection.mutable
import scala.slick.SlickException

object PgTokenHelper {

  sealed trait Token {
    def value: String
  }
  case class GroupToken(members: List[Token]) extends Token {
    val value = ""
    override def toString = {
      StringBuilder.newBuilder append "GroupToken(" append {
        members map { m => m.toString } mkString (",")
      } append ")" toString
    }
  }

  case object Comma extends Token {
    val value = ","
    override def toString = "Comma"
  }
  case object Null extends Token {
    val value = ""
    override def toString = "Null"
  }
  case class  Chunk (value: String) extends Token {
    override def toString = s"Chunk($value)"
  }
  case class  Escape(value: String) extends Token {
    override def toString = s"Escape($value)"
  }

  trait Border extends Token {
    def marker: String
  }
  case class Open (value: String, marker: String = "") extends Border {
    override def toString = s"Open($marker$value)"
  }
  case class Close(value: String, marker: String = "") extends Border {
    override def toString = s"Close($value$marker)"
  }
  case class Marker(marker: String) extends Border {
    val value = ""
    override def toString = s"Marker($marker)"
  }

  ///////////////////////////////////////////////////////////////////
  private case class WorkingGroup(border: Border, level: Int) {
    val tokens = mutable.ListBuffer[Token]()
  }

  def getString(token: Token, level: Int): String = {
    def unescape(value: String, level: Int): String = {
      val step = math.pow(2, level).toInt
      (for(i <- (-1 + step) to (value.length, step)) yield value.charAt(i))
        .mkString("")
    }

    def mergeString(buf: mutable.StringBuilder, token: Token): Unit =
      token match {
        case GroupToken(mList) => mList.foreach(mergeString(buf, _))
        case Escape(v) => buf append unescape(v, level + 1)
        case Marker(m) => buf append unescape(m, level + 1)
        case t => buf append t.value
      }

    ///
    val buf = StringBuilder.newBuilder
    mergeString(buf, token)
    buf.toString
  }

  def getChildren(token: Token): List[Token] = token match {
    case GroupToken(mList) => mList.filterNot(_.isInstanceOf[Border]).filterNot(_ == Comma)
    case _ => throw new IllegalArgumentException("WRONG token type: " + token)
  }

  def createString(root: Token): String = {
    val MARK_REQUIRED_CHAR_LIST = List('\\', '"', ',', '(', ')')
    val rootIsArray = root match {
      case GroupToken(mList) =>
        mList match {
          case Open("{", _) :: tail => true
          case _ => false
        }
      case _ => false
    }

    ///
    def isMarkRequired(token: Token): Boolean = token match {
      case g: GroupToken => true
      case Chunk(v) => v.find(MARK_REQUIRED_CHAR_LIST.contains).isDefined
      case _ => false
    }

    def appendMark(buf: mutable.StringBuilder, level: Int) = 
      if (level >= 0) {
        val markLen = math.pow(2, level).toInt
        level match {
          case 0 => buf append "\""
          case 1 => buf append (if (rootIsArray) "\\\"" else "\"\"")
          case 2 => buf append (if (rootIsArray) "\\\"\\\"" else "\\\\\"\"")
          case _ => buf append ("\\" * (markLen -4)) append (if (rootIsArray) "\\\"\\\"" else "\\\\\"\"")
        }
      }

    def appendEscaped(buf: mutable.StringBuilder, ch: Char, level: Int) = 
      if (level < 0) buf append ch
      else {
        val escapeLen = math.pow(2, level +1).toInt
        ch match {
          case '\\' => buf append ("\\" * escapeLen)
          case '"'  => buf append ("\\" * (escapeLen -1)) append '"'
          case _  =>   buf append ch
        }
      }

    def mergeString(buf: mutable.StringBuilder, token: Token, level: Int): Unit = {
      if (isMarkRequired(token)) appendMark(buf, level)
      token match {
        case GroupToken(mList) => {
          buf append mList(0).value
          var isFirst = true
          for(i <- 1 to (mList.length -2)) {
            if (isFirst) isFirst = false else buf append ","
            mergeString(buf, mList(i), level +1)
          }
          buf append mList.last.value
        }
        case Chunk(v) => v.map(appendEscaped(buf, _, level))
        case _  =>  //nothing to do
      }
      if (isMarkRequired(token)) appendMark(buf, level)
    }

    ///
    val buf = StringBuilder.newBuilder
    mergeString(buf, root, -1)
    buf.toString
  }

  def grouping(tokens: List[Token]): Token = {
    def level(marker: String): Double =
      math.log(marker.length) / math.log(2)

    def isCompatible(open: Border, close: Border) =
      (open, close) match {
        case (Open("(", m1), Close(")", m2)) if m1 == m2 => true
        case (Open("(", m1), Close("]", m2)) if m1 == m2 => true
        case (Open("[", m1), Close(")", m2)) if m1 == m2 => true
        case (Open("[", m1), Close("]", m2)) if m1 == m2 => true
        case (Open("{", m1), Close("}", m2)) if m1 == m2 => true
        case (_, _)      =>      false
      }

    ///
    val stack = mutable.Stack[WorkingGroup]()
    stack.push(WorkingGroup(Marker(""), -1))

    for(i <- 0 until tokens.length) {
      tokens(i) match {
        //-- process head and last tokens
        case t if (i == 0 || i == tokens.length -1) => stack.top.tokens += t
        //-- insert Null token if necessary
        case Comma => {
          if (tokens(i-1) == Comma || tokens(i-1).isInstanceOf[Open]) {
            stack.top.tokens += Null
          }
          stack.top.tokens += Comma
          if (tokens(i+1).isInstanceOf[Close]) {
            stack.top.tokens += Null
          }
        }
        //-- process open tokens
        // '{' + '{' -> multi-dimension array, 'ttt{' -> normal string
        case t @ Open("{", "") => {
          if (tokens(i-1).value == "{") {
            stack.push(WorkingGroup(t, stack.top.level))
            stack.top.tokens += t
          } else stack.top.tokens += Chunk("{")
        }
        // open border should prefix a marker, negative case: ',"tt(a...' <--> ',"(tt...' (normal)
        case Open(v, "") => stack.top.tokens += Chunk(v)
        // mark + escape, case: ',"\"(...",' <--> ',"...",' (normal)
        case Open(v, m) if (level(m) != math.round(level(m))) => {
          val index = math.pow(2, stack.top.level).toInt
          stack.push(WorkingGroup(Marker(m.substring(0, index)), stack.top.level +1))
          stack.top.tokens += Marker(m.substring(0, index)) += Escape(m.substring(index)) += Chunk(v)
        }
        case t @ Open(v, m) => {
          if (tokens(i-1) == Comma || tokens(i-1).isInstanceOf[Open]) {
            stack.push(WorkingGroup(t, stack.top.level +1))
            stack.top.tokens += t
          } // case: 'tt\"(...'
          else stack.top.tokens += Escape(m) += Chunk(v)
        }
        //-- process marker tokens
        // mark + escape
        case Marker(m) if (level(m) != math.round(level(m)) && tokens(i-1) == Comma) => {
          val index = math.pow(2, stack.top.level).toInt
          stack.push(WorkingGroup(Marker(m.substring(0, index)), stack.top.level +1))
          stack.top.tokens += Marker(m.substring(0, index)) += Escape(m.substring(index))
        }
        // escape + mark
        case Marker(m) if (level(m) != math.round(level(m)) && tokens(i+1) == Comma) => {
          val existed = stack.find(g => m.endsWith(g.border.marker)).get
          for (_ <- 0 to stack.lastIndexOf(existed)) {
            if (stack.top == existed) {
              val index = m.length - stack.top.border.marker.length
              stack.top.tokens += Escape(m.substring(0, index)) += Marker(m.substring(index))
            }
            val toBeMerged = GroupToken(stack.pop.tokens.toList)
            stack.top.tokens += toBeMerged
          }
        }
        // mark + escape + mark
        case Marker(m) if (tokens(i-1) == Comma && tokens(i+1) == Comma) => {
          val topMarker = stack.top.border.marker
          if ((m.length > topMarker.length * 2) && m.startsWith(topMarker) && m.endsWith(topMarker)) {
            stack.top.tokens += Escape(m.substring(topMarker.length -1, m.length -topMarker.length))
          } else stack.top.tokens += Escape(m)
        }
        case t @ Marker(m) => {
          val existed = stack.find(g => g.border.marker == m)
          if (existed.isDefined) {
            for (_ <- 0 to stack.lastIndexOf(existed.get)) {
              if (stack.top == existed.get) stack.top.tokens += t
              val toBeMerged = GroupToken(stack.pop.tokens.toList)
              stack.top.tokens += toBeMerged
            }
          } else {
            stack.push(WorkingGroup(t, stack.top.level +1))
            stack.top.tokens += t
          }
        }
        //-- process close tokens
        // '{' + '{' -> multi-dimension array, 'ttt{' -> normal string
        case Close("}", "") if (tokens(i+1).value != "}") => stack.top.tokens += Chunk("}")
        // close border should postfix a marker, negative case: ',"...)ttt' <--> ',"...)\",' (normal)
        case Close(v, "") if (v != "}") => stack.top.tokens += Chunk(v)
        // escape + mark, case: '...tt)\"",' <--> '...tt)",..' (normal)
        case Close(v, m)  if (level(m) != math.round(level(m))) => {
          val existed = stack.find(b => m.endsWith(b.border.marker)).get
          for (_ <- 0 to stack.lastIndexOf(existed)) {
            if (stack.top == existed) {
              val index = m.length - stack.top.border.marker.length
              stack.top.tokens += Chunk(v) += Escape(m.substring(0, index)) += Marker(m.substring(index))
            }
            val toBeMerged = GroupToken(stack.pop.tokens.toList)
            stack.top.tokens += toBeMerged
          }
        }
        case t @ Close(v, m) => {
          if ((tokens(i+1) == Comma || tokens(i+1).isInstanceOf[Close]) && isCompatible(stack.top.border, t)) {
            stack.top.tokens += t
            val toBeMerged = GroupToken(stack.pop.tokens.toList)
            stack.top.tokens += toBeMerged
          } // case: ',"}ttt...'
          else stack.top.tokens += Chunk(v) += Escape(m)
        }
        //-- process other tokens
        case t => stack.top.tokens += t
      }
    }

    ///
    GroupToken(stack.top.tokens.toList)
  }

  ////////////////////////////////////////////////////////////////////////////
  object Tokenizer extends RegexParsers {
    override def skipWhitespace = false

    val MARKER = """[\\|"]+""".r
    val ESCAPE = """\\+[^"\\]""".r
    val CHUNK  = """[^}){(\[\]\\,"]+""".r

    def open: Parser[Token] = opt(MARKER) ~ (elem('{') | elem('(') | elem('[')) ^^ {
      case (x ~ y) => Open(String.valueOf(y), x.getOrElse(""))
    }

    def close: Parser[Token] = (elem('}') | elem(')') | elem(']')) ~ opt(MARKER) ^^ {
      case (x ~ y) => Close(String.valueOf(x), y.getOrElse(""))
    }

    def escape = ESCAPE ^^ { x => Escape(x) }
    def marker = MARKER ^^ { x => Marker(x) }
    def comma = elem(',') ^^ { x => Comma }
    def chunk = CHUNK ^^ { x => Chunk(x) }

    def patterns = open | close | escape | marker | comma | chunk

    //--
    def tokenize(input: String) =
      parseAll(rep(patterns), new CharSequenceReader(input)) match {
        case Success(result, _) => result
        case failure: NoSuccess => throw new SlickException(failure.msg)
      }

  }
}