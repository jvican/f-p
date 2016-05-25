package fp.util

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

trait AllTestEnvironment
    extends Matchers
    with BootHelper
    with Assertions
    with ScalaFutures
    with BeforeAndAfter {
  this: Suite =>

  implicit val patience = PatienceConfig(20.seconds, 1.second)

}

trait FpSpec extends fixture.FlatSpec with AllTestEnvironment

trait SimpleFpSpec extends FlatSpec with AllTestEnvironment
