package fp.backend.netty

import fp._
import fp.model.pickling.PicklingProtocol
import fp.util.FpSpec

import scala.spores._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class PopulateTest extends FpSpec {

  import PicklingProtocol._

  val worker = bootReadyServer
  implicit val master = bootReadyClient

  val s = spore[Unit, Silo[List[String]]] { Unit =>
    new Silo(List("1", "2", "3"))
  }

  "Populate" should "create a new Silo in the server" in {

    val data = new SiloFactory(s)
    val action = data.populateAt(serverHost)
    whenReady(action){
      case sr => sr shouldBe a[SiloRef[List[String]]]
    }

    Await.ready(shutDownEverything(master, worker), 5.seconds)
  }
}
