package com.hypertino.binders.json

import com.hypertino.binders.json.api._

import scala.collection.mutable
import scala.scalajs.js

class JsParserAdapter(value: Any) extends JsonParserApi {
  private var _currentToken: JsToken = JsUnknown
  private var _currentIterator: Option[JsParserIterator] = None
  private var isEof = false
  private var _stringValue: String = _
  private var _numberValue: BigDecimal = _
  private var _fieldName: Option[String] = None
  private val iteratorStack = mutable.Stack[JsParserIterator]()

  override def currentToken: JsToken = _currentToken
  override def fieldName: Option[String] = _fieldName
  override def stringValue: String = _stringValue
  override def numberValue: BigDecimal = _numberValue
  override def location: String = iteratorStack.map(_.toString).mkString("/") + currentToken.toString + "/" + fieldName

  override def nextToken(): JsToken = {
    if (!isEof) {
      _currentIterator match {
        case None ⇒
          updateTo(value)

        case Some(objIt: JsParserObjectIterator) ⇒
          nextObjectElement(objIt)

        case Some(arrIt: JsParserArrayIterator) ⇒
          nextArrayElement(arrIt)
      }
    } else {
      clear()
    }
    currentToken
  }

  private def nextObjectElement(it: JsParserObjectIterator) = {
    currentToken match {
      case JsFieldName =>
        updateTo(it.currentValue)

      case _ =>
        if (!it.isEof) {
          _currentToken = JsFieldName
          _fieldName = Some(it.fieldName)
          _stringValue = null
          _numberValue = null
        }
        else {
          popIterator()
          _currentToken = JsEndObject
        }
    }
  }

  private def nextArrayElement(it: JsParserArrayIterator) = {
    if (!it.isEof) {
      updateTo(it.currentValue)
    }
    else {
      popIterator()
      _currentToken = JsEndArray
    }
  }

  private def clear(): Unit = {
    _currentIterator = None
    _stringValue = null
    _numberValue = null
    _currentToken = JsUnknown
    _fieldName = None
  }

  private def popIterator(): Unit = {
    clear()
    if (iteratorStack.isEmpty) {
      isEof = true
    } else {
      _currentIterator = Some(iteratorStack.pop())
    }
  }

  private def updateTo(v: Any): Unit = {
    val it = _currentIterator
    clear()
    v match {
      case s: String =>
        _currentToken = JsString
        _stringValue = s
      case n: Double =>
        _currentToken = JsNumber
        _numberValue = BigDecimal(n)
      case true =>
        _currentToken = JsTrue
      case false =>
        _currentToken = JsFalse
      case null =>
        _currentToken = JsNull
      case s: js.Array[_] =>
        _currentToken = JsStartArray
        _currentIterator = Some(JsParserArrayIterator(s, 0))
      case s: js.Object =>
        _currentToken = JsStartObject
        _currentIterator = Some(JsParserObjectIterator(s.asInstanceOf[js.Dictionary[_]].toVector, 0))
    }
    it.foreach { previousIterator ⇒
      if (_currentIterator.isDefined)
        iteratorStack.push(previousIterator.advance())
      else
        _currentIterator = Some(previousIterator.advance())
    }
    if (_currentIterator.isEmpty) {
      isEof = true
    }
  }
}

sealed trait JsParserIterator {
  def currentValue: Any
  def advance(): JsParserIterator
  def isEof: Boolean
}
case class JsParserObjectIterator(obj: Vector[(String, Any)], index: Int) extends JsParserIterator {
  override def isEof: Boolean = index >= obj.size
  override def advance(): JsParserIterator = copy(obj, index+1)
  override def currentValue: Any = obj(index)._2
  override def toString = {
    if (isEof) "{}" else s"{$fieldName}"
  }
  def fieldName: String = obj(index)._1
}

case class JsParserArrayIterator(arr: js.Array[_], index: Int) extends JsParserIterator {
  override def isEof: Boolean = index >= arr.size
  override def advance(): JsParserIterator = copy(arr, index+1)
  override def currentValue: Any = arr(index)
  override def toString = {
    if (isEof) "[]" else s"[$index]"
  }
}

