package fp.util

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertions, FlatSpec, Matchers}

import scala.concurrent.duration._

trait FpSpec
    extends FlatSpec
    with Matchers
    with BootHelper
    with Assertions
    with ScalaFutures {
  implicit val patience = PatienceConfig(10.seconds, 1.second)
}
