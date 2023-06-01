package pt.tecnico.dsi.openstack.neutron.models

import cats.Show
import cats.derived.derived
import com.comcast.ip4s.{Cidr, IpAddress}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.{Decoder, Encoder}

object Route:
  given encoder[IP <: IpAddress: Encoder]: Encoder[Route[IP]] = ConfiguredEncoder.derived
  given decoder[IP <: IpAddress: Decoder](using Decoder[Cidr[IP]]): Decoder[Route[IP]] = ConfiguredDecoder.derived

  given ordering[IP <: IpAddress: Ordering]: Ordering[Route[IP]] = Ordering.by(x => (x.destination, x.nexthop))
  
  given show[IP <: IpAddress: Show]: Show[Route[IP]] = Show.derived
final case class Route[+IP <: IpAddress](destination: Cidr[IP], nexthop: IP)
