package fp.backend.netty

import fp._
import fp.model.pickling.PicklingProtocol

import scala.spores._
import scala.concurrent.ExecutionContext.Implicits.global

class PopulateTest extends ClientServerFpSpec {

  import PicklingProtocol._

  val s = spore[Unit, Silo[List[String]]] { Unit =>
    new Silo(List("1", "2", "3"))
  }

  "Populate" should "create a new Silo in the server" in { ctx =>

    implicit val c = ctx.client

    val data = new SiloFactory(s)
    val action = data.populateAt(serverHost)

    whenReady(action){
      case sr => sr shouldBe a[SiloRef[_]]
    }

  }
}
