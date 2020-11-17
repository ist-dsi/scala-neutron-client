package pt.tecnico.dsi.openstack.neutron.models

import cats.Show
import cats.syntax.show._
import com.comcast.ip4s.{Cidr, IpAddress}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object AllocationPool {
  /** Creates an AllocationPool from `cidr`. */
  def fromCidr[IP <: IpAddress](cidr: Cidr[IP]): AllocationPool[IP] =
    AllocationPool(cidr.prefix.next.next, cidr.last.previous).asInstanceOf[AllocationPool[IP]]
  
  def fromCidrAndGateway[IP <: IpAddress](cidr: Cidr[IP], gateway: Option[IP] = None): Option[(IP, List[AllocationPool[IP]])] = {
    val (network, broadcast) = (cidr.prefix, cidr.last) // These addresses are reserved
    val gatewayIP = gateway.getOrElse(network.next.asInstanceOf[IP])
    if (!cidr.contains(gatewayIP) || gatewayIP == network || gatewayIP == broadcast) {
      None
    } else {
      val (fistAvailable, lastAvailable) = (network.next, broadcast.previous)
      val pools = gatewayIP match {
        case `fistAvailable` => List(AllocationPool(gatewayIP.next, lastAvailable))
        case `lastAvailable` => List(AllocationPool(fistAvailable, gatewayIP.previous))
        case _ => List(AllocationPool(fistAvailable, gatewayIP.previous), AllocationPool(gatewayIP.next, lastAvailable))
      }
      Some((gatewayIP, pools.asInstanceOf[List[AllocationPool[IP]]]))
    }
  }
  
  implicit def encoder[IP <: IpAddress: Encoder]: Encoder[AllocationPool[IP]] = deriveEncoder(renaming.snakeCase)
  implicit def decoder[IP <: IpAddress: Decoder]: Decoder[AllocationPool[IP]] = deriveDecoder(renaming.snakeCase)
  
  implicit def ordering[IP <: IpAddress: Ordering]: Ordering[AllocationPool[IP]] = Ordering.by(x => (x.start, x.end))
  
  implicit def show[IP <: IpAddress: Show]: Show[AllocationPool[IP]] = Show.show(pool => s"${pool.start.show}-${pool.end.show}")
}
case class AllocationPool[+IP <: IpAddress](start: IP, end: IP)
