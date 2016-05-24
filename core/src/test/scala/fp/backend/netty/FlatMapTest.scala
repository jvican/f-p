package fp.backend.netty

import fp._
import fp.util.FpSpec
import fp.backend.netty.{Server => NettyServer}
import fp.core.Materialized
import fp.model.pickling.PicklingProtocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.spores._

class FlatMapTest extends FpSpec {

  import PicklingProtocol._

  implicit val master = bootReadyClient
  val worker = bootReadyServer.asInstanceOf[SiloSystem with NettyServer]

  "Flatmap" should "should work over a Silo" in {

    // Set up the silo in the server
    val silo = new Silo(List("1", "2", "3"))
    val refId = SiloRefId(serverHost)
    worker.silos += refId -> silo

    // Set up the DAG representing
    val dag = Materialized(refId)

    // Set up SiloRef
    val sr = new MaterializedSilo[List[String]](dag, serverHost)

    val action: SiloRef[List[Int]] = sr.flatMap(spore {
      (l: List[String]) =>
        new Silo(l.map(_.toInt))
    })

    whenReady(action.send) {
      _ shouldBe List(1, 2, 3)
    }

    Await.ready(shutDownEverything(master, worker), 5.seconds)

  }
}
