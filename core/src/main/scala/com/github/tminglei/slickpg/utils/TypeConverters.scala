package com.github.tminglei.slickpg
package utils

import scala.reflect.runtime.{universe => ru, currentMirror => runtimeMirror}
import scala.reflect.ClassTag
import scala.slick.util.Logging
import java.sql.{Timestamp, Time, Date}
import java.util.UUID
import java.text.SimpleDateFormat

object TypeConverters extends Logging {
  @scala.annotation.implicitNotFound(msg = "No converter available for ${FROM} to ${TO}")
  trait TypeConverter[FROM, TO] extends (FROM => TO)

  object TypeConverter {
    def apply[FROM, TO](convert: (FROM => TO)) =
      new TypeConverter[FROM, TO] {
        def apply(v: FROM) = convert(v)
      }
  }

  ///////////////////////////////////////////////////////////////
  private val converterMap = collection.concurrent.TrieMap[CacheKey, TypeConverter[_, _]]()

  private[utils] def internalGet(from: ru.Type, to: ru.Type) = {
    val cacheKey = CacheKey(from, to)
    logger.debug(s"get converter for ${from.erasure} => ${to.erasure}")
    converterMap.get(cacheKey).orElse({
      if (to <:< from) {
        converterMap += (cacheKey -> TypeConverter((v: Any) => v))
        converterMap.get(cacheKey)
      } else None
    })
  }

  def register[FROM,TO](convert: (FROM => TO))(implicit from: ru.TypeTag[FROM], to: ru.TypeTag[TO]) = {
    logger.info(s"register converter for ${from.tpe.erasure} => ${to.tpe.erasure}")
    converterMap += (CacheKey(from.tpe, to.tpe) -> TypeConverter(convert))
  }

  def converter[FROM,TO](implicit from: ru.TypeTag[FROM], to: ru.TypeTag[TO]): TypeConverter[FROM,TO] = {
    internalGet(from.tpe, to.tpe).map(_.asInstanceOf[TypeConverter[FROM,TO]])
      .getOrElse(throw new IllegalArgumentException(s"Converter NOT FOUND for ${from.tpe} => ${to.tpe}"))
  }

  ///
  private[utils] case class CacheKey(val from: ru.Type, val to: ru.Type) {
    override def hashCode(): Int = {
      from.erasure.hashCode() * 31 + to.erasure.hashCode()
    }
    override def equals(o: Any) = {
      if (o.isInstanceOf[CacheKey]) {
        val that = o.asInstanceOf[CacheKey]
        from =:= that.from && to =:= that.to
      } else false
    }
  }

  //////////////////////////////////////////////////////////////////////////
  object Util {
    import PGObjectTokenizer.PGElements._

    private def dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
    private def timeFormatter = new SimpleDateFormat("HH:mm:ss")
    private def tsFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private def pgBoolAdjust(s: String): String =
      Option(s).map(_.toLowerCase) match {
        case Some("t")  => "true"
        case Some("f")  => "false"
        case _ => s
      }

    lazy val registerBasicTypeConverters = synchronized {
      register((v: String) => v.toInt)
      register((v: String) => v.toLong)
      register((v: String) => v.toShort)
      register((v: String) => v.toFloat)
      register((v: String) => v.toDouble)
      register((v: String) => pgBoolAdjust(v).toBoolean)
      register((v: String) => v.toByte)
      register((v: String) => UUID.fromString(v))
      // register date/time converters
      register((v: String) => new Date(dateFormatter.parse(v).getTime))
      register((v: String) => new Time(timeFormatter.parse(v).getTime))
      register((v: String) => new Timestamp(tsFormatter.parse(v).getTime))
      register((v: Date) => dateFormatter.format(v))
      register((v: Time) => timeFormatter.format(v))
      register((v: Timestamp) => tsFormatter.format(v))
      true
    }

    ///
    def mkCompositeConvFromString[T <: AnyRef](implicit ev: ru.TypeTag[T]): TypeConverter[String, T] = {
      registerBasicTypeConverters
      new TypeConverter[String, T] {
        val thisType = ru.typeOf[T]
        if (internalGet(elemType, thisType).isEmpty)
          register(mkConvFromElement[T])
        
        val convFromElement = internalGet(elemType, thisType).map(_.asInstanceOf[TypeConverter[Element,T]]).get
        def apply(str: String): T = {
          convFromElement(PGObjectTokenizer.tokenize(str))
        }
      }
    }
    
    def mkCompositeConvToString[T <: AnyRef](implicit ev: ru.TypeTag[T], ev1: ClassTag[T]): TypeConverter[T, String] = {
      registerBasicTypeConverters
      new TypeConverter[T, String] {
        val thisType = ru.typeOf[T]
        if (internalGet(thisType, elemType).isEmpty)
          register(mkConvToElement[T])

        val convToElement = internalGet(thisType, elemType).map(_.asInstanceOf[TypeConverter[T,Element]]).get
        def apply(v: T): String = {
          PGObjectTokenizer.reverse(convToElement(v))
        }
      }
    }

    ///
    def mkArrayConvFromString[T](implicit ev: ru.TypeTag[T]): TypeConverter[String, List[T]] = {
      registerBasicTypeConverters
      new TypeConverter[String, List[T]] {
        val thisType = ru.typeOf[T]
        internalGet(strType, thisType).getOrElse(throw new IllegalArgumentException(s"Converter NOT FOUND for String => $thisType"))

        val convFromElement = mkArrayConvFromElement[T]
        def apply(str: String): List[T] = {
          convFromElement(PGObjectTokenizer.tokenize(str))
        }
      }
    }

    def mkArrayConvToString[T](implicit ev: ru.TypeTag[T], ev1: ClassTag[T]): TypeConverter[List[T], String] = {
      registerBasicTypeConverters
      new TypeConverter[List[T], String] {
        val convToElement = mkArrayConvToElement[T]
        def apply(vList: List[T]): String = {
          PGObjectTokenizer.reverse(convToElement(vList))
        }
      }
    }

    //////////////////////////////////////// support methods ////////////////
    private val strType  = ru.typeOf[String]
    private val elemType = ru.typeOf[Element]

    private def convertToValue(e: Element, toType: ru.Type): Any = e match {
      case ValueE(v)  => {
        TypeConverters.internalGet(strType, toType).get.asInstanceOf[TypeConverter[String,_]](v)
      }
      case _: CompositeE  => {
        TypeConverters.internalGet(elemType, toType).get.asInstanceOf[TypeConverter[Element,_]](e)
      }
      case ArrayE(eList) => {
        val eType = toType.asInstanceOf[ru.TypeRef].args(0)
        eList.map(e => convertToValue(e, eType))
      }
      case _ /* should be null */ => null
    }

    private def convertToElement(v: Any, fromType: ru.Type): Element = {
      (v, fromType) match {
        case (Some(realVal), _) => {
          val realType = fromType.asInstanceOf[ru.TypeRef].args(0)
          convertToElement(realVal, realType)
        }
        case (vList: List[_], _) => {
          val vType = fromType.asInstanceOf[ru.TypeRef].args(0)
          ArrayE(vList.map(convertToElement(_, vType)))
        }
        case (_, _) => {
          TypeConverters.internalGet(fromType, elemType)
            .map(_.asInstanceOf[TypeConverter[Any, Element]](v))
            .getOrElse(if (v == null || v == None) NullE else ValueE(v.toString) )
        }
      }
    }

    ///
    private[utils] def mkConvFromElement[T <: AnyRef](implicit ev: ru.TypeTag[T]): TypeConverter[Element, T] = {
      registerBasicTypeConverters
      new TypeConverter[Element, T] {
        private val thisType = ru.typeOf[T]
        private val optType = ru.typeOf[Option[_]]

        private val constructor = thisType.declaration(ru.nme.CONSTRUCTOR).asMethod
        private val ctorArgInfo = constructor.paramss.head.map(_.typeSignature).map(tpe =>
          if (tpe.typeConstructor =:= optType.typeConstructor) {
            val realType = tpe.asInstanceOf[ru.TypeRef].args(0)
            (realType, true)
          } else (tpe, false)
        )

        //--
        def apply(elem: Element): T = {
          val classMirror = runtimeMirror.reflectClass(thisType.typeSymbol.asClass)
          val ctorMirror = classMirror.reflectConstructor(constructor)

          val args = elem.asInstanceOf[CompositeE].members.zip(ctorArgInfo).map {
            case (e, (tpe, isOption)) => {
              val tv = convertToValue(e, tpe)
              if (isOption) Option(tv) else tv
            }
          }
          ctorMirror.apply(args: _*).asInstanceOf[T]
        }
      }
    }

    private[utils] def mkConvToElement[T](implicit ev: ru.TypeTag[T], ev1: ClassTag[T]): TypeConverter[T, Element] = {
      registerBasicTypeConverters
      new TypeConverter[T, Element] {
        private val thisType = ru.typeOf[T]

        private val constructor = thisType.declaration(ru.nme.CONSTRUCTOR).asMethod
        private val ctorFieldInfo = constructor.paramss.head.map(t => {
          val field = thisType.declaration(t.name).asTerm
          val tpe  = t.typeSignature
          (field, tpe)
        })

        //--
        def apply(v: T): Element = {
          val instanceMirror = runtimeMirror.reflect(v)
          CompositeE(
            ctorFieldInfo.map {
              case (field, tpe) => {
                val fv = instanceMirror.reflectField(field).get
                convertToElement(fv, tpe)
              }
            }
          )
        }
      }
    }

    ////
    private[utils] def mkArrayConvFromElement[T](implicit ev: ru.TypeTag[T]): TypeConverter[Element, List[T]] = {
      registerBasicTypeConverters
      new TypeConverter[Element, List[T]] {
        private val thisType = ru.typeOf[T]
        private val optType = ru.typeOf[Option[_]]

        private val (realType, isOption) =
          if (thisType.typeConstructor =:= optType.typeConstructor) {
            val realType = thisType.asInstanceOf[ru.TypeRef].args(0)
            (realType, true)
          } else (thisType, false)

        //--
        def apply(elem: Element): List[T] = {
          elem.asInstanceOf[ArrayE].elements.map { e =>
            val tv = convertToValue(e, realType)
            if (isOption) Option(tv) else tv
          } map (_.asInstanceOf[T])
        }
      }
    }

    private[utils] def mkArrayConvToElement[T](implicit ev: ru.TypeTag[T], ev1: ClassTag[T]): TypeConverter[List[T], Element] = {
      registerBasicTypeConverters
      new TypeConverter[List[T], Element] {
        private val thisType = ru.typeOf[T]

        def apply(vList: List[T]): Element = {
          ArrayE(
            vList.map { v =>
              convertToElement(v, thisType)
            }
          )
        }
      }
    }
  }
}
