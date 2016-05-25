package fp.util

import scala.pickling._
import scala.pickling.pickler.AnyPicklerUnpickler

object PicklingHelper {

  def writeTemplate[T](builder: PBuilder, field: String, value: T,
                       pickler: Pickler[T], sideEffect: PBuilder => Unit) = {
    builder.putField(field, { b =>
      sideEffect(b)
      pickler.pickle(value, b)
    })
  }

  def write[T](builder: PBuilder, field: String, value: T, pickler: Pickler[T]) =
    writeTemplate(builder, field, value, pickler, {b => ()})

  def writeEliding[T](builder: PBuilder, field: String, value: T, pickler: Pickler[T]) =
    writeTemplate(builder, field, value, pickler, {b =>
      b.hintElidedType(pickler.tag)
    })

  def readTemplate[T](reader: PReader, field: String,
                      unpickler: Unpickler[T], sideEffect: PReader => Unit): T = {
    val reader1 = reader.readField(field)
    if(!reader1.atPrimitive) {
      sideEffect(reader1)
      val tag1 = reader1.beginEntry()
      val result = unpickler.unpickle(tag1, reader1).asInstanceOf[T]
      reader1.endEntry()
      result
    } else reader1.readPrimitive().asInstanceOf[T]
  }

  def read[T](reader: PReader, field: String, unpickler: Unpickler[T]): T =
    readTemplate(reader, field, unpickler, {r => ()})

  final val anyTag = AnyPicklerUnpickler.tag
  def readEliding[T](reader: PReader, field: String, unpickler: Unpickler[T]): T =
    readTemplate(reader, field, unpickler, {r =>
      if(unpickler.tag != anyTag)
        r.hintElidedType(unpickler.tag)
    })

}
