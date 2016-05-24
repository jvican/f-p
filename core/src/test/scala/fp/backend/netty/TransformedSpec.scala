package fp.backend.netty

import fp.backend.SelfDescribing
import fp.model.Transformed
import fp.model.pickling.PicklingProtocol

class TransformedSpec extends ClientFpSpec {

  import PicklingProtocol._
  implicit val master = bootReadyClient

  val confirmation = Transformed[Any](
    master.MsgIdGen.next,
    master.systemId,
    List(1, 2, 3, 4, 5)
  )


  it should "be picklable/unpicklable even if static picklers have been used" in { ctx =>

    println((List(1,2,3): Any).pickle.value)
    val m1 = SelfDescribing(confirmation)
    val result = m1.pickle
    println(result)
    val m2 = result.unpickle[SelfDescribing]
    assert(m1 === m2)

  }

}
