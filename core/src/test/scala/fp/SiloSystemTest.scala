package fp

import fp.backend.netty.SiloSystem
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

class SiloSystemTest extends FlatSpec with Matchers {

  def initSilo = Await.ready(SiloSystem(), 10.seconds)

  "Instantiation" should "yield a default silo system" in {
    initSilo map { _ shouldBe a[SiloSystem] }
  } 

  it should "throw an exception in case of wrong `silo.system.impl` parameter" in {
    val osp = Option(System.getProperty("silo.system.impl"))

    System.setProperty("silo.system.impl", "XXX")
    initSilo map { _ shouldBe a[ClassNotFoundException] }

    osp match {
      case None    => System.clearProperty("silo.system.impl")
      case Some(v) => System.setProperty("silo.system.impl", v)
    }
  }

  it should "throw an exception in case of port is already taken" in {
    val system = Await.result(SiloSystem(Some(9999)), 10.seconds)
    Try(Await.result(SiloSystem(Some(9999)), 10.seconds)) match {
      case Success(s) =>
        Await.result(s.terminate(), 10.seconds)
        true shouldBe false 
      case Failure(e) => e shouldBe a[java.net.BindException]
    }
    Await.result(system.terminate(), 10.seconds)
  }

}

