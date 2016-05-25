package fp
package backend

import java.net.InetSocketAddress
import java.util.concurrent.CancellationException

import fp.model.Message

import io.netty.channel._
import io.netty.util.concurrent.{Future => NettyFuture, FutureListener}

import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions

package object netty {

  type NettyContext = ChannelHandlerContext

  /** Wrapper around any message of the internal function-passing protocol that
    * stores a [[ChannelHandlerContext]] to give further information to Netty.
    *
    * @param ctx Netty context
    * @param msg Function-passing model
    */
  private[netty] final case class NettyWrapper(ctx: NettyContext, msg: Message)
      extends WrappedMsg[NettyContext]
      with Comparable[NettyWrapper] {
    // Ugly because we use java PriorityBlockingQueue and requires Comparable
    def compareTo(m2: NettyWrapper): Int =
      Ordering.Int.compare(m2.msg.id.value, msg.id.value)
  }

  import language.implicitConversions
  implicit def wrappedToNettyWrapper(
      wrp: WrappedMsg[NettyContext]): NettyWrapper =
    NettyWrapper(wrp.ctx, wrp.msg)

  /** Describe a connection between two nodes in a given network.
    * It can be either [[Connected]] or [[Disconnected]].
    */
  private[netty] sealed abstract class State
  private[netty] case object Disconnected extends State
  private[netty] final case class Connected(
      channel: Channel,
      worker: EventLoopGroup
  ) extends State

  /** Conversion between an [[ChannelFuture]] and a Scala [[Future]]. */
  implicit def toScalaFutureChannel(future: ChannelFuture): Future[Channel] = {
    val p = Promise[Channel]()
    future.addListener(new ChannelFutureListener {
      override def operationComplete(future: ChannelFuture): Unit = {
        if (future.isSuccess) p.success(future.channel)
        else if (future.isCancelled) p.failure(new CancellationException)
        else p.failure(future.cause)
      }
    })
    p.future
  }

  /** Conversion between an [[NettyFuture]] and a Scala [[Future]]. */
  implicit def nettyFutureToScalaFuture[T](future: NettyFuture[T]): Future[T] = {
    val p = Promise[T]()
    future.addListener(new FutureListener[T] {
      override def operationComplete(future: NettyFuture[T]): Unit = {
        if (future.isSuccess) p.success(future.get)
        else if (future.isCancelled) p.failure(new CancellationException)
        else p.failure(future.cause)
      }
    })
    p.future
  }

  /** Enrich a NettyContext and add explicit method to get the remote host. */
  implicit class EnrichedContext(ctx: NettyContext) {
    def getRemoteHost: InetSocketAddress =
      ctx.channel.remoteAddress.asInstanceOf[InetSocketAddress]
  }
}
