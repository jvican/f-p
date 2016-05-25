package fp.backend.netty

import java.util.concurrent.{BlockingQueue, CountDownLatch, TimeUnit}

import com.typesafe.scalalogging.{StrictLogging => Logging}
import fp.backend.netty.handlers.{PopulateHandler, TransformHandler}
import fp.model._
import fp.util.AsyncExecution

import scala.concurrent.{ExecutionContext, Future}

private[netty] class Receptor(
                               incoming: BlockingQueue[NettyWrapper],
                               startSignal: CountDownLatch
)(
    implicit val ec: ExecutionContext,
    server: Server,
    system: SiloSystem
)
    extends Runnable
    with AsyncExecution
    with Logging {

  import logger._


  /** Responsible for controlling the status of the [[Receptor]] */
  private val receptorStatus = new CountDownLatch(2)
  @inline private final val Running = 2

  /** Handle incoming [[fp.model.Message]]s by pattern matching on their
    * types and spawning [[scala.concurrent.Future]]s that will carry out
    * its processing. There should be a [[fp.backend.netty.handlers.Handler]]
    * for each type of message.
    */
  def handleMsg(wrapper: NettyWrapper): Unit = {
    val NettyWrapper(ctx, msg) = wrapper
    (msg match {
      case p: Populate[_] => PopulateHandler.handle(p, ctx)
      case m: Transform => TransformHandler.handle(m, ctx)
      case _ => Future.failed(new Exception(s"We weren't able to handle $msg"))
    }) onFailure {
      case e: Throwable =>
        error(s"Exception happened $e.\n${e.getStackTrace.mkString("\n")}")
    }
  }

  import server.{MessagingStatus, OnHoldMessages}

  private def updateStatusOf(status: MessagingStatus): MessagingStatus = {
    val (previousExpectedId, msgs) = status
    (previousExpectedId.increaseByOne, msgs)
  }

  private def processStoredMsgs(current: MessagingStatus) = {

    @scala.annotation.tailrec
    def process(nextId: Int, msgs: OnHoldMessages): MessagingStatus = {
      val nextMsg = msgs.peek()
      if (nextMsg != null && nextMsg.msg.id.value == nextId) {
        msgs.poll()
        handleMsg(nextMsg)
        process(nextId + 1, msgs)
      } else {
        (MsgId(nextId), msgs)
      }
    }

    val (expectedId, onHold) = current
    process(expectedId.value, onHold)
  }

  /** Confirm the last sent message to a given host. The last message must have
    * been lost on its way since the client has sent again an already processed
    * message. Therefore, reconfirm it. This conforms to the ACK-Reply protocol. */
  def confirmMsgAgain(ctx: NettyContext, senderId: SiloSystemId): Unit = {
    val lastResponse = server.unconfirmedResponses(senderId)
    server.sendAndForget(ctx.channel, lastResponse)
  }

  @inline def processIncomingMsg(wrappedMsg: NettyWrapper) = {
    debug(s"Receptor received: $wrappedMsg")
    val host = wrappedMsg.msg.senderId

    val status = server.statusFrom(host)
    val (expectedId, onHoldMsgs) = status
    val msgId = wrappedMsg.msg.id.value
    val expectedMsgId = expectedId.value

    if (msgId == expectedMsgId) {
      /* Client and server are on the same page */
      handleMsg(wrappedMsg)
      val updatedStatus = updateStatusOf(status)
      server.statusFrom += (host -> updatedStatus)
      processStoredMsgs(updatedStatus)
    } else if (msgId == expectedMsgId - 1) {
      /* Message's already been processed */
      confirmMsgAgain(wrappedMsg.ctx, host)
    } else if (msgId > expectedMsgId) {
      /* Received a future message since its id is greater
       * than the expected. Store for future processing. */
      onHoldMsgs put wrappedMsg
    } else {
      error(Feedback.receptionAlgorithmFailed)
    }
  }

  def start(): Unit = {
    startSignal.countDown()
    trace("Receptor started.")

    while (receptorStatus.getCount == Running) {
      val possibleMsg = {
        try Option(incoming.poll(100, TimeUnit.MILLISECONDS))
        catch { case e: InterruptedException => None }
      }

      if (possibleMsg.isDefined)
        processIncomingMsg(possibleMsg.get)
    }

    // Signal that receptor dies
    receptorStatus.countDown()
    trace("Receptor stopped.")
  }

  def stop(): Unit = {
    trace("Stopping receptor...")
    receptorStatus.countDown()
    // Waits until receptor is dead
    receptorStatus.await()
  }

  final override def run() = start()
}
