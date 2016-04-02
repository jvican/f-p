/**
 * This package, [[fp]], contains ...
 */
package object fp {

  /**
   * A [[https://en.wikipedia.org/wiki/Host_(network) network host]] is a
   * computer or other device connected to a computer network. A network host
   * may offer information resources, services, and applications to users or
   * other nodes on the network. It is assigned a network layer host address.
   *
   * @param address Host address
   * @param port Host port
   */
  final case class Host(address: String, port: Int) {

    override val toString = s"$address:$port"

  }

  def debug(msg: String) = println(msg)

}

