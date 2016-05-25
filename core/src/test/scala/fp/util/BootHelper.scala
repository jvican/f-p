package fp.util

import fp.Host
import fp.backend.SiloSystem
import fp.backend.netty.{SiloSystem => NettySiloSystem}
import org.scalatest.Assertions

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

trait BootHelper {
  this: Assertions =>

  implicit val serverHost = Host("localhost", 3243)

  def bootReadyServer(implicit ec: ExecutionContext) =
    Await.result(bootServer(ec), 10.seconds)

  /** Throws assert exception if [[SiloSystem]] is not properly booted up */
  def bootServer(implicit ec: ExecutionContext): Future[SiloSystem] = {
    val p = Promise[SiloSystem]()
    NettySiloSystem(serverHost) onComplete {
      case Success(sys) => p.success(sys)
      case Failure(err) => p.failure(err)
    }
    p.future
  }

  def bootReadyClient = Await.result(bootClient, 10.seconds)
  def bootClient: Future[SiloSystem] = NettySiloSystem()

  def shutDownSystem(sys: SiloSystem) = sys.terminate
  def shutDownEverything(client: SiloSystem, server: SiloSystem) = {
    shutDownSystem(client).flatMap(_ => shutDownSystem(server)).recoverWith {
      case t: Throwable => shutDownSystem(server)
    }
  }
}
