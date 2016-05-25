package fp

import fp.backend.SiloSystem
import fp.util.SimpleFpSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

class SiloSystemTest extends SimpleFpSpec {

  "Instantiation of a SiloSystem" should "yield a new one" in {
    whenReady(bootClient) {
      case s =>
        s shouldBe a[SiloSystem]
        Await.result(shutDownSystem(s), 5.seconds)
    }
  }

  it should "throw an exception if port is already taken" in {
    whenReady(bootServer) {
      case s =>
        bootServer map { s =>
          Await.result(shutDownSystem(s), 5.seconds)
          fail("Starting a server in the same port should fail")
        } recover {
          case t =>
            intercept[java.net.BindException] {
              throw t
            }
        }
        Await.result(shutDownSystem(s), 5.seconds)
    }
  }
}
