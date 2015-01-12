package eu.inn.binders.json

import java.util.Date

import com.fasterxml.jackson.core.JsonGenerator
import eu.inn.binders.core.Serializer
import eu.inn.binders.json.internal.JsonMacro
import eu.inn.binders.naming.{Converter}
import scala.language.experimental.macros

class JsonSerializer[C <: Converter](val jsonGenerator: JsonGenerator) extends Serializer[C]{
  def getFieldSerializer(fieldName: String): Option[JsonSerializer[C]] = {
    jsonGenerator.writeFieldName(fieldName)
    Some(createFieldSerializer())
  }

  protected def createFieldSerializer() = new JsonSerializer[C](jsonGenerator)

  def writeNull(): Unit = jsonGenerator.writeNull()
  def writeInteger(value: Int): Unit = jsonGenerator.writeNumber(value)
  def writeLong(value: Long): Unit = jsonGenerator.writeNumber(value)
  def writeString(value: String): Unit = jsonGenerator.writeString(value)
  def writeFloat(value: Float): Unit = jsonGenerator.writeNumber(value)
  def writeDouble(value: Double): Unit = jsonGenerator.writeNumber(value)
  def writeBoolean(value: Boolean): Unit = jsonGenerator.writeBoolean(value)
  def writeBigDecimal(value: BigDecimal): Unit = jsonGenerator.writeNumber(value.bigDecimal)
  def writeDate(value: Date): Unit = jsonGenerator.writeNumber(value.getTime)
  def writeMap[T](value: Map[String,T]) = macro JsonMacro.writeMap[JsonSerializer[C], T]

  def beginObject(): Unit = {
    jsonGenerator.writeStartObject()
  }

  def endObject(): Unit = {
    jsonGenerator.writeEndObject()
  }

  def beginArray(): Unit = {
    jsonGenerator.writeStartArray()
  }
  def endArray(): Unit = {
    jsonGenerator.writeEndArray()
  }
}
