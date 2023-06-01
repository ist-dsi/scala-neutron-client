package pt.tecnico.dsi.openstack.neutron.models

import cats.Show
import cats.syntax.show.*
import com.comcast.ip4s.{Cidr, IpAddress}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.{Decoder, Encoder}

object AllocationPool:
  def lastAvailableAddress[IP <: IpAddress](cidr: Cidr[IP]): IP = cidr.last.fold(
    v4 = _.previous, // In IPv4 the last address is reserved for the broadcast address. So the last available address is the previous one.
    v6 = identity
  ).asInstanceOf[IP]
  
  /** Creates an AllocationPool from `cidr`. */
  def fromCidr[IP <: IpAddress](cidr: Cidr[IP]): AllocationPool[IP] =
    // `cidr.prefix` is the network address
    // `cidr.prefix.next` is the gateway address
    // so `cidr.prefix.next.next` is the first available address for the allocation pool
    AllocationPool(cidr.prefix.next.next, lastAvailableAddress(cidr)).asInstanceOf[AllocationPool[IP]]
  
  def fromCidrAndGateway[IP <: IpAddress](cidr: Cidr[IP], gateway: Option[IP] = None): Option[(IP, List[AllocationPool[IP]])] =
    val (network, broadcast) = (cidr.prefix, cidr.last) // These addresses are reserved
    val gatewayIP = gateway.getOrElse(network.next.asInstanceOf[IP])
    if !cidr.contains(gatewayIP) || gatewayIP == network || gatewayIP == broadcast then
      None
    else
      val (fistAvailable, lastAvailable) = (network.next, lastAvailableAddress(cidr))
      
      val pools = gatewayIP match
        case `fistAvailable` => List(AllocationPool(gatewayIP.next, lastAvailable))
        case `lastAvailable` => List(AllocationPool(fistAvailable, gatewayIP.previous))
        case _ => List(AllocationPool(fistAvailable, gatewayIP.previous), AllocationPool(gatewayIP.next, lastAvailable))
      Some((gatewayIP, pools.asInstanceOf[List[AllocationPool[IP]]]))
  
  given [IP <: IpAddress: Encoder]: Encoder[AllocationPool[IP]] = ConfiguredEncoder.derived
  given [IP <: IpAddress: Decoder]: Decoder[AllocationPool[IP]] = ConfiguredDecoder.derived
  
  given ordering[IP <: IpAddress: Ordering]: Ordering[AllocationPool[IP]] = Ordering.by(x => (x.start, x.end))
  
  given [IP <: IpAddress: Show]: Show[AllocationPool[IP]] = Show.show(pool => s"${pool.start.show}-${pool.end.show}")
case class AllocationPool[+IP <: IpAddress](start: IP, end: IP)
