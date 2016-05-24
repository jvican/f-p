package fp

import fp.backend.netty.SiloSystem
import fp.util.{BootHelper, SimpleFpSpec}
import org.scalatest.{Assertions, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

class SiloSystemTest extends SimpleFpSpec {

  "Instantiation of a SiloSystem" should "yield a new one" in {
    bootClient map { _ shouldBe a[SiloSystem] }
  }

  it should "throw an exception if `silo.system.impl` param is wrong" in {
    val osp = Option(System.getProperty("silo.system.impl"))

    System.setProperty("silo.system.impl", "XXX")
    bootClient map { _ shouldBe a[ClassNotFoundException] }

    osp match {
      case None => System.clearProperty("silo.system.impl")
      case Some(v) => System.setProperty("silo.system.impl", v)
    }
  }

  it should "throw an exception if port is already taken" in {
    val system = bootServer

    whenReady(system) { case _ =>
      intercept[java.net.BindException] {
        Await.result(bootServer, 5.seconds)
      }
    }

    Await.result(system.map(_.terminate()), 5.seconds)
  }
}
