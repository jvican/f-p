package fp.model.pickling

import fp.backend.SelfDescribing
import fp.model.Transformed
import fp.util.FpSpec

class TransformedSpec extends FpSpec {

  import PicklingProtocol._
  implicit val master = bootReadyClient

  val confirmation = Transformed[Any](
    master.MsgIdGen.next,
    master.systemId,
    List(1, 2, 3, 4, 5)
  )


  it should "be picklable/unpicklable even if static picklers have been used" in {

    println((List(1,2,3): Any).pickle.value)
    val m1 = SelfDescribing(confirmation)
    val result = m1.pickle
    println(result)
    val m2 = result.unpickle[SelfDescribing]
    assert(m1 === m2)

  }

}
