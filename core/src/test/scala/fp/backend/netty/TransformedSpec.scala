package fp.backend.netty

import fp.model.Transformed
import fp.model.pickling.PicklingProtocol

class TransformedSpec extends ClientFpSpec {

  import PicklingProtocol._

  it should "be pickled/unpickled as any" in { ctx =>

    val client = ctx.client

    val tr = Transformed[Any](
      client.MsgIdGen.next,
      client.systemId,
      List(1, 2, 3, 4, 5)
    )

    val result = (tr: Any).pickle
    val tr2 = result.unpickle[Any]
    assert(tr === tr2)

  }

}
