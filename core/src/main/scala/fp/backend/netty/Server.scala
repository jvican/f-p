package fp
package backend
package netty

import java.net.BindException

import fp.model.{Message, SiloSystemId}
import com.typesafe.scalalogging.{StrictLogging => Logging}
import java.util.concurrent.{CountDownLatch, FutureTask, LinkedBlockingQueue}

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel._
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.logging.{LogLevel, LoggingHandler => Logger}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.{global => executor}

private[netty] trait Server
    extends backend.Server
    with SiloWarehouse
    with MessagingLayer[NettyContext]
    with Sender
    with Logging {

  self: SiloSystem =>

  import logger._

  /* Promise the server is up and running. */
  protected def promiseOfStart: Promise[SiloSystem]

  /* Netty server constituents */
  private val server = new ServerBootstrap
  private val bossGrp = new NioEventLoopGroup
  private val wrkrGrp = new NioEventLoopGroup

  private val startupLatch = new CountDownLatch(1)

  /** System message processing constituents.
    *
    * `receptor` : Worker for all incoming messages from all channels.
    * `forwarder`: Server handler that forwards messages received by Netty.
    */
  private val mq = new LinkedBlockingQueue[NettyWrapper]
  private val receptor = new Receptor(mq, startupLatch)(ec, this, self)

  /* Not thread-safe, only accessed in the [[Receptor]].
   * Be careful, [[ConcurrentMap]] could be better than [[TrieMap]] */
  override lazy val statusFrom = initMessagingHub

  /* Thread-safe since it's accessed in the handlers */
  override lazy val unconfirmedResponses =
    TrieMap.empty[SiloSystemId, SelfDescribing]

  /* Thread-safe since it's accessed in the handlers */
  override lazy val silos = TrieMap.empty[SiloRefId, Silo[_]]

  def shutDownWorkers: Future[Unit] = {
    val f1: Future[Any] = bossGrp.shutdownGracefully()
    val f2: Future[Any] = wrkrGrp.shutdownGracefully()

    Future.sequence(Vector(f1, f2)) map (_ => ())
  }

  /** Initialize a Netty-based server. */
  def initServer() = {
    trace("Server initializing...")

    try {
      server
        .group(bossGrp, wrkrGrp)
        .channel(classOf[NioServerSocketChannel])
        .childHandler(new ChannelInitializer[SocketChannel]() {
          override def initChannel(ch: SocketChannel): Unit = {
            val pipeline = ch.pipeline()
            pipeline.addLast(new Logger(LogLevel.TRACE))
            pipeline.addLast(new LengthFieldPrepender(4))
            pipeline.addLast(new Encoder())
            pipeline.addLast(new Decoder())
            pipeline.addLast(new ServerHandler())
          }
        })
        .option(ChannelOption.SO_BACKLOG.asInstanceOf[ChannelOption[Any]], 128)
        .childOption(
          ChannelOption.SO_KEEPALIVE.asInstanceOf[ChannelOption[Any]], true)
        .validate()
    } catch { case t: Throwable =>
        error("Failure when initializing the server", t)
        // FIXME Another approach to this blocking op?
        Await.result(shutDownWorkers, 5.seconds)
    }

    trace("Server initializing done.")
  }

  // Init server at startup
  initServer()

  /* Forward incoming messages from Netty's event loop to internal
   * message queue processed by [[Receptor]].
   *
   * Be aware that due to the default constructor parameter of
   * [[io.netty.channel.SimpleChannelInboundHandler]] all messages will be
   * automatically released by passing them to
   * [[io.netty.util.ReferenceCountUtil#release(Object)]].
   */
  @ChannelHandler.Sharable
  private class ServerHandler()
      extends SimpleChannelInboundHandler[Message]
      with Logging {

    override def channelRead0(ctx: ChannelHandlerContext, msg: Message): Unit =
      mq add NettyWrapper(ctx, msg)

    override def exceptionCaught(
        ctx: ChannelHandlerContext, cause: Throwable): Unit = {
      //cause.printStackTrace()

      // XXX Don't just close the connection when an exception is raised.
      //     With the current setup of the channel pipeline, in order to respond
      //     with a raw text message --- recall only a
      //     silo sytem can create system messages due to `Id` --- additional
      //     encoder/ decoder are required.
      val msg = s"${cause.getClass.getSimpleName}: ${cause.getMessage}"
      logger.error(s"$msg\n$cause")
      ctx.close()

      //import ChannelFutureListener._
      //if (ctx.channel().isActive())
      //  ctx.writeAndFlush(msg).addListener(CLOSE)
      //else ()
    }
  }

  /** Start and bind server to accept incoming connections at port `at.port` */
  override def start(): Unit = {
    trace("Server start...")
    val bound: Future[SiloSystem] = try {
      server bind host.toAddress sync() map { _ =>
        trace(s"Server bound to $host"); self
      }
    } catch {
      case t: Throwable =>
        error("Failure when starting the server", t)
        stop()
        (promiseOfStart failure t).future
    }

    promiseOfStart completeWith {
      bound map { s =>
        executor execute receptor
        startupLatch.await()
        trace("Server start done.")
        s // Return the server
      }
    }
  }

  /** Stop server.
    *
    * In Netty 4.0, you can just call `shutdownGracefully` on the
    * `EventLoopGroup` that manages all your channels. Then all ''existing
    * channels will be closed automatically'' and reconnection attempts should
    * be rejected.
    */
  override def stop(): Unit = {
    trace("Server stop...")
    // Only stop receptor if it's been started
    if(startupLatch.getCount == 0) receptor.stop()
    // FIXME Temporary fix to startup/shutdown issues
    bossGrp.shutdownGracefully().getNow
    wrkrGrp.shutdownGracefully().getNow
    trace("Server stop done.")
  }
}
