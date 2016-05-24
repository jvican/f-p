package fp.backend.netty

import fp._
import fp.util.FpSpec

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait ClientFpSpec extends FpSpec {

  case class FixtureParam(client: backend.SiloSystem)

  override def withFixture(test: OneArgTest) = {
    implicit val client = bootReadyClient
    try test(FixtureParam(client))
    finally Await.ready(shutDownSystem(client), 10.seconds)
  }

}

trait ClientServerFpSpec extends FpSpec {

  type NettySystemServer = SiloSystem with Server
  case class FixtureParam(
      client: backend.SiloSystem, server: NettySystemServer)

  override def withFixture(test: OneArgTest) = {
    implicit val client = bootReadyClient
    // Will need to change if the official backend is not netty
    val server = bootReadyServer.asInstanceOf[NettySystemServer]
    try test(FixtureParam(client, server))
    finally Await.ready(shutDownEverything(client, server), 10.seconds)
  }

}
