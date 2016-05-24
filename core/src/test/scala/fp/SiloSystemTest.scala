package fp

import fp.backend.netty.SiloSystem
import fp.util.BootHelper
import org.scalatest.{Assertions, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

class SiloSystemTest
    extends FlatSpec
    with Matchers
    with BootHelper
    with Assertions {

  "Instantiation" should "yield a default silo system" in {
    bootClient map { _ shouldBe a[SiloSystem] }
  }

  it should "throw an exception in case of wrong `silo.system.impl` parameter" in {
    val osp = Option(System.getProperty("silo.system.impl"))

    System.setProperty("silo.system.impl", "XXX")
    bootClient map { _ shouldBe a[ClassNotFoundException] }

    osp match {
      case None => System.clearProperty("silo.system.impl")
      case Some(v) => System.setProperty("silo.system.impl", v)
    }
  }

  it should "throw an exception when a port is already taken" in {
    val system = bootServer

    Try(bootServer) match {
      case Success(s) =>
        Await.result(s.map(_.terminate()), 10.seconds)
        fail("Booting server in the same port should fail")
      case Failure(e) =>
        e.getMessage should include ("Address already in use")
    }

    Await.result(system.map(_.terminate()), 10.seconds)
  }
}
