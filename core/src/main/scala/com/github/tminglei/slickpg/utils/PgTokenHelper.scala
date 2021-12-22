package com.github.tminglei.slickpg.utils

import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable.{ListBuffer, Stack}
import scala.annotation.tailrec

object PgTokenHelper {

  sealed trait Token {
    def value: String
  }
  case class GroupToken(members: Seq[Token]) extends Token {
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

  trait Border extends Token
  case class Open (value: String) extends Border {
    override def toString = s"Open($value)"
  }
  case class Close(value: String) extends Border {
    override def toString = s"Close($value)"
  }
  case class Marker(value: String) extends Border {
    override def toString = s"Marker($value)"
  }

  ////////////////////////////////////////////////////////////////////////////

  def isCompatible(open: Token, token: Token): Boolean =
    (open, token) match {
      case (Open("("), Close(")")) => true
      case (Open("["), Close("]")) => true
      case (Open("{"), Close("}")) => true
      case (Open("("), Close("]")) => true  // for range type
      case (Open("["), Close(")")) => true  // for range type
      case (_, _)                  => false
    }


  def getString(token: Token, level: Int): String = {
    def unescape(value: String, level: Int): String = {
      val step = math.pow(2, level).toInt
      (for(i <- (-1 + step) until (value.length, step)) yield value.charAt(i))
        .mkString("")
    }

    def mergeString(buf: StringBuilder, token: Token): Unit =
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

  @tailrec
  private def smush(soFar: Vector[Token], remaining: Seq[Token]): Seq[Token] = (soFar, remaining) match {
    case (tokens, Seq())                                => tokens
    case (empty, head +: tail) if empty.isEmpty         => smush(Vector(head), tail)
    case (lead :+ Chunk(prefix), Chunk(suffix) +: tail) => smush(lead :+ Chunk(prefix + suffix), tail)
    case (lead, middle +: tail)                         => smush(lead :+ middle, tail)
  }
  
  def getChildren(token: Token): Seq[Token] = token match {
    case GroupToken(mList) => 
      smush(Vector.empty, mList)
        // I don't know why, but CompositeConverter.fromToken is going to 
        // throw an IllegalArgumentException: argument type mismatch 
        // if you don't make this a list right here and now
        // no, getChildren(tokens).toList will not work
        .toList 
        .filterNot(_ == null)
        .filterNot(_.isInstanceOf[Border])
        .filterNot(_ == Comma)
    case _ => throw new IllegalArgumentException("WRONG token type: " + token)
  }

  def createString(root: Token): String = {
    val MARK_REQUIRED_CHAR_LIST = List('\\', '"', ',', '(', ')', '{', '}')

    ///
    def isArray(token: Token): Boolean = token match {
      case GroupToken(mList) =>
        mList match {
          case Open("{") :: _ => true
          case _ => false
        }
      case _ => false
    }

    def isMarkRequired(token: Token, parentIsArray: Boolean): Boolean = token match {
      case _: GroupToken => !parentIsArray || !isArray(token)
      case Chunk(v) => v.isEmpty || v.trim.length < v.length || "NULL".equalsIgnoreCase(v) || v.find(MARK_REQUIRED_CHAR_LIST.contains).isDefined
      case _ => false
    }

    val rootIsArray = isArray(root)
    def appendMark(buf: StringBuilder, level: Int): Unit =
      if (level >= 0) {
        val markLen = math.pow(2, level).toInt
        level match {
          case 0 => buf append "\""
          case 1 => buf append (if (rootIsArray) "\\\"" else "\"\"")
          case 2 => buf append (if (rootIsArray) "\\\"\\\"" else "\\\\\"\"")
          case _ => buf append ("\\" * (markLen -4)) append (if (rootIsArray) "\\\"\\\"" else "\\\\\"\"")
        }
      }

    def appendEscaped(buf: StringBuilder, ch: Char, level: Int): Unit =
      if (level < 0) buf append ch
      else {
        val escapeLen = math.pow(2, level +1).toInt
        ch match {
          case '\\' => buf append ("\\" * escapeLen)
          case '"'  => buf append ("\\" * (escapeLen -1)) append '"'
          case _  =>   buf append ch
        }
      }

    def mergeString(buf: StringBuilder, token: Token, level: Int, parentIsArray: Boolean): Unit = {
      val markRequired = isMarkRequired(token, parentIsArray)
      if (markRequired) appendMark(buf, level)
      token match {
        case GroupToken(mList) => {
          val vList = mList.filterNot(_.isInstanceOf[Marker])
          if (vList.size > 2 && isCompatible(vList.head, vList.last)) {
            buf append vList.head.value
            var isFirst = true
            for (i <- 1 to (vList.length -2)) {
              if (isFirst) isFirst = false else buf append ","
              mergeString(buf, vList(i), level +1, isArray(token))
            }
            buf append vList.last.value
          } else {
            for (t <- vList) {
              t.value.map(appendEscaped(buf, _, level))
            }
          }
        }
        case Chunk(v) => v.map(appendEscaped(buf, _, level))
        case Null if parentIsArray => buf append "null"
        case _  =>  //nothing to do
      }
      if (markRequired) appendMark(buf, level)
    }

    ///
    val buf = StringBuilder.newBuilder
    mergeString(buf, root, -1, isArray(root))
    buf.toString
  }

  def grouping(tokens: List[Token]): Token = {
    case class WorkingGroup(open: Open, marker: Marker, level: Int) {
      val tokens = ListBuffer[Token]()
      val isInChunk = (open == null && marker.value.nonEmpty)
    }

    def toContentToken(token: Token): Token =
      token match {
        case b: Border =>
          if (b.isInstanceOf[Marker])
            Escape(b.value)
          else Chunk(b.value)
        case other  => other
      }

    def isDiggable(wg: WorkingGroup, token: Token, tails: List[Token],
            resultRef: AtomicReference[(Open, Marker, Escape, List[Token])]): Boolean = {
      val mLength = math.pow(2, wg.level).toInt
      token match {
        case open @ Open(_) if !wg.isInChunk =>
          resultRef.set((open, Marker(""), null, tails))
          true
        case marker @ Marker(m) if !wg.isInChunk && (m.length >= mLength && (m.length / mLength) % 2 == 1) =>
          if (m.length == mLength) {
            tails match {
              case (open @ Open(_)) :: tail =>
                resultRef.set((open, marker, null, tail))
                true
              case _  =>
                resultRef.set((null, marker, null, tails))
                true
            }
          } else {
            val mStr = m.substring(0, mLength)
            val eStr = m.substring(mLength)
            resultRef.set((null, Marker(mStr), Escape(eStr), tails))
            true
          }
        case _ => false
      }
    }

    def isClosable(stack: Stack[WorkingGroup], token: Token, tails: List[Token],
            resultRef: AtomicReference[(Close, Marker, Escape, Int, List[Token])]): Boolean = {
      token match {
        case c @ Close(_) if isCompatible(stack.top.open, c) =>
          if (stack.top.marker.value.nonEmpty && (tails.nonEmpty && stack.top.marker.value == tails.head.value)) {
            resultRef.set((c, Marker(tails.head.value), null, 0, tails.tail))
            true
          } else if (stack.top.marker.value.isEmpty) {
            resultRef.set((c, Marker(""), null, 0, tails))
            true
          } else {
            false
          }
        case marker @ Marker(m) if m.length > 0 =>
          var closable = false
          var stoppable = false
          for (i <- 0 until stack.length if !stoppable) {
            val mLength = stack(i).marker.value.length
            closable = {
              if (mLength > 0 && (m.length >= mLength && (m.length / mLength) % 2 == 1)) {
                val escaped = if (m.length > mLength) Escape(m.substring(0, m.length - mLength)) else null
                val nMarker = if (m.length > mLength) Marker(m.substring(m.length - mLength)) else marker
                resultRef.set((null, nMarker, escaped, i, tails))
                true
              } else {
                false
              }
            }
            stoppable = closable || mLength <= m.length
          }
          closable
        case _ => false
      }
    }

    def groupForward(stack: Stack[WorkingGroup], tokens: List[Token]): Unit = {
      val diggResultRef  = new AtomicReference[(Open, Marker, Escape, List[Token])]()
      val closeResultRef = new AtomicReference[(Close, Marker, Escape, Int, List[Token])]()
      val mLength = math.pow(2, stack.top.level).toInt
      tokens match {
        // for empty string
        case Marker(m) :: tail if !stack.top.isInChunk && m.length == mLength * 2 =>
          val m2 = m.substring(0, m.length / 2)
          stack.top.tokens += GroupToken(List(Marker(m2), Chunk(""), Marker(m2)))
          groupForward(stack, tail)

        // for null value: '..,,..' / '..,)'
        case Comma :: sep2 :: tail if !stack.top.isInChunk && (sep2 == Comma || sep2.isInstanceOf[Close]) =>
          stack.top.tokens += Comma += Null
          groupForward(stack, sep2 :: tail)

        // for open characters in strings
        case Chunk(c) :: Open(o) :: tail => 
          stack.top.tokens += Chunk(c + o)
          groupForward(stack, tail)

        // for diggable
        case (head: Border) :: tail if !stack.top.isInChunk && isDiggable(stack.top, head, tail, diggResultRef) =>
          val (open, marker, escaped, tails) = diggResultRef.get()
          val level = if (marker.value.isEmpty) stack.top.level else stack.top.level +1 // keep level if marker is empty

          val newWorkingGroup = WorkingGroup(open, marker, level)
          newWorkingGroup.tokens += marker += open += escaped // open and escaped can't be non-empty at same time.
          // fill in null value for case '\"(,..'
          if (open != null && (tails.nonEmpty && tails.head == Comma))
            newWorkingGroup.tokens += Null

          stack.push(newWorkingGroup)
          groupForward(stack, tails)

        // for closable
        case (head: Border) :: tail if isClosable(stack, head, tail, closeResultRef) =>
          val (close, marker, escaped, sIndex, tails) = closeResultRef.get()
          // merge wrong digged group
          for (_ <- 0 until sIndex -1) {
            stack.top.tokens ++= stack.pop().tokens.map(toContentToken)
          }

          stack.top.tokens += escaped += close += marker  // close and escaped can't be non-empty at same time.
          val toBeMerged = GroupToken(stack.pop().tokens.toList.filterNot(_ == null))
          stack.top.tokens += toBeMerged
          groupForward(stack, tails)

        // for normal tokens
        case head :: tail =>
          stack.top.tokens += toContentToken(head)
          groupForward(stack, tail)

        // for end back
        case Nil  => // do nothing
      }
    }

    ///
    val stack = Stack[WorkingGroup]()
    stack.push(WorkingGroup(null, Marker(""), 0))
    groupForward(stack, tokens)
    stack.top.tokens(0)
  }

  ////////////////////////////////////////////////////////////////////////////
  import scala.util.parsing.combinator.RegexParsers
  import scala.util.parsing.input.CharSequenceReader
  import slick.SlickException

  object Tokenizer extends RegexParsers {
    override def skipWhitespace = false

    val MARKER = """[\\|"]+""".r
    val ESCAPE = """\\+[^"\\]""".r
    val CHUNK  = """[^}){(\[\]\\,"]+""".r

    def open: Parser[Token] = (elem('{') | elem('(') | elem('[')) ^^ {
      case (x) => Open(String.valueOf(x))
    }

    def close: Parser[Token] = (elem('}') | elem(')') | elem(']')) ^^ {
      case (x) => Close(String.valueOf(x))
    }

    def escape = ESCAPE ^^ { x => Escape(x) }
    def marker = MARKER ^^ { x => Marker(x) }
    def comma = elem(',') ^^ { _ => Comma }
    def chunk = CHUNK ^^ { x => Chunk(x) }

    def patterns = open | close | escape | marker | comma | chunk

    //--
    def tokenize(input: String): List[Token] = {
      parseAll(rep(patterns), new CharSequenceReader(input)) match {
        case Success(result, _) => result
        case failure: NoSuccess => throw new SlickException(failure.msg)
      }
    }
  }
}
