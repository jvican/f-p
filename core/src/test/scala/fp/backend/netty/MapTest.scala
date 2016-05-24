package fp.backend.netty

import fp._
import fp.core.Materialized
import fp.model.pickling.PicklingProtocol

import scala.spores._

class MapTest extends ClientServerFpSpec {

  import PicklingProtocol._

  "Map" should "work over a silo" in { ctx =>

    implicit val c = ctx.client

    // Set up the silo in the server
    val silo = new Silo(List("1", "2", "3"))
    val refId = SiloRefId(serverHost)
    ctx.server.silos += refId -> silo

    // Set up the DAG representing
    val dag = Materialized(refId)

    // Set up SiloRef
    val sr = new MaterializedSilo[List[String]](dag, serverHost)

    val action: SiloRef[List[Int]] = sr.map(spore {
      (l: List[String]) =>
        l.map(_.toInt)
    })

    whenReady(action.send) {
      _ shouldBe List(1, 2, 3)
    }

  }
}
