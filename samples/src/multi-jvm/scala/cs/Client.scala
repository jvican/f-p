package multijvm
package cs

import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{ Success, Failure }

import com.typesafe.scalalogging.{ StrictLogging => Logging }

import silt._

/** A silo system running in client mode can be understood as ''master'', or
  *  ''driver''.
  * 
  * All these terms have their raison d'être, i.e., all these terms, in general,
  * state the fact that this node is for defining the control flow, the to be
  * executed computations and for sending those computations to respective
  * slaves or executors.
  */ 
object ExampleMultiJvmClient extends AnyRef with Logging {

  private val summary = """
In this talk, I'll present some of our ongoing work on a new programming model
for asynchronous and distributed programming. For now, we call it
"function-passing" or "function-passing style", and it can be thought of as an
inversion of the actor model - keep your data stationary, send and apply your
functionality (functions/spores) to that stationary data, and get typed
communication all for free, all in a friendly collections/futures-like package!
"""

  private lazy val words: Array[String] =
    summary.replace('\n', ' ').split(" ")

  def word(random: scala.util.Random): String = {
    val index = random.nextInt(words.length)
    words(index)
  }

  val numLines = 10

  // each string is a concatenation of 10 random words, separated by space
  val data = () => {
    val buffer = collection.mutable.ListBuffer[String]()
    val random = new scala.util.Random(100)
    for (i <- 0 until numLines) yield {
      val tenWords = for (_ <- 1 to 10) yield word(random)
      buffer += tenWords.mkString(" ")
    }
    new Silo(buffer.toList)
  }

  def main(args: Array[String]): Unit = { 
    /* Start a silo system in client mode.
     */
    val system = SiloSystem() match {
      case Success(system) => Await.result(system, 10.seconds)
      case Failure(error)  => sys.error(s"Could not instantiate silo system:\n ${error.getMessage}")
    }
    logger.info(s"Silo system in client mode up and running (${system.name}).")

    /* Specify the location where to publish data.
     */
    val target = Host("127.0.0.1", 8090)

    //import scala.pickling.Defaults._
    //import scala.pickling.shareNothing._

    ///* Populate some data.
    // */
    //system.populate(data)(target)

    ////val siloFut = system.fromFun(host)(() => populateSilo(10, new scala.util.Random(100)))
    ////val done = siloFut.flatMap(_.send())

    ////val res = Await.result(done, 15.seconds)
    ////println("RESULT:")
    ////println(s"size of list: ${res.size}")
    ////res.foreach(println)

    logger.info(s"Silo system in client mode connecting to `$target`...")
    system.connect(target) map { _ =>
      logger.info(s"Silo system in client mode connecting to `$target` done.")
      Thread.sleep(1000)

      logger.info(s"Silo system in client mode terminating...")
      try {
        val result = Await.result(system.terminate(), 10.seconds)
        logger.info(s"Silo system in client mode terminated with message `$result`.")
      } catch {
        case error: Throwable => logger.error(s"Silo system in client mode terminated with error `${error.getMessage}`.")
      }
    }
  }
 
}

// vim: set tw=80 ft=scala:
