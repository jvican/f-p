package fp

import scala.concurrent.Future

trait SiloFactory[T] {

  def data: T

}

class Silo[T](private[fp] val data: T) {

  //def internalApply[A, V, B <: Traversable[V]](fun: A => B): LocalSilo[V, B] = {
  //  val typedFun = fun.asInstanceOf[T => B]
  //  println(s"LocalSilo: value = $value")
  //  val res = typedFun(value)
  //  println(s"LocalSilo: result of applying function: $res")
  //  new LocalSilo[V, B](res)
  //}

  //def send(): Future[T] = {
  //  Future.successful(value)
  //}

  //def doPumpTo[A, B](existFun: Function2[A, Emitter[B], Unit], emitter: Emitter[B]): Unit = {
  //  val fun = existFun.asInstanceOf[Function2[U, Emitter[B], Unit]]
  //  Future {
  //    value.foreach { elem =>
  //      // println(s"visiting element $elem")
  //      fun(elem, emitter)
  //    }
  //    emitter.done()
  //  }
  //}

}

