package fp
package backend

import fp.model.pickling.PicklingProtocol

import scala.pickling._

import PicklingProtocol._
import fp.util.RuntimeHelper
import sporesPicklers._

import scala.pickling.pickler.AnyPicklerUnpickler

/** Wrapper useful to allow deserialization of certain cases in which
  * scala-pickling cannot generate pickler/unpicklers for.
  *
  * It is necessary since there are some subclasses of `Message` that
  * are parameterised and thus cannot be deserialized in the recipient.
  *
  * The `SelfDescribing` instance solves this problem. We rely on runtime
  * execution to get the proper unpickler for a message, but we know
  * beforehand that such unpickler has already been created (since all the
  * nodes present in the network share the same jars) and therefore we
  * statically unpickle the message. This is a workaround to avoid dynamic
  * deserialization (hint: bad performance) and stick with static picklers.
  */
case class SelfDescribing(key: String, blob: String) {


  /* All the magic happens here, we get the static pickler and
   * unpickle the blob. The reader is necessary to store the
   * unpickled object and cast it to a concrete type `T`.
   *
   * Be careful, `T` has to be the actual type of the unpickled
   * object, which is Any, otherwise bad things may happen... */
  def unpickleWrapped[T]: T = {

    import scala.pickling.json.JSONPickle

    val blobPickle = JSONPickle(blob)
    val reader = pickleFormat.createReader(blobPickle)
    AnyPicklerUnpickler.unpickle(key, reader).asInstanceOf[T]

  }

}

object SelfDescribing {

  import fp.model.Message

  /** Direct creation of `SelfDescribing` from a `Message`. This
    * pattern is used in `Tell` and `Ask` when we serialize messages. */
  def apply[M <: Message: Pickler: Unpickler](msg: M): SelfDescribing = {

    val unpickler = implicitly[Unpickler[M]]
    val pickled = msg.pickle.value
    SelfDescribing(unpickler.tag.key, pickled)

  }


}

