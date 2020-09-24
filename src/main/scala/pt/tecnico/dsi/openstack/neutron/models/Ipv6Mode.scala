package pt.tecnico.dsi.openstack.neutron.models

import enumeratum.{Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

sealed trait Ipv6Mode extends EnumEntry
case object Ipv6Mode extends Enum[Ipv6Mode] {
  implicit val circeEncoder: Encoder[Ipv6Mode] = Encoder[String].contramap {
    case Slaac => "slaac"
    case Dhcpv6Stateful => "dhcpv6-stateful"
    case Dhcpv6Stateless => "dhcpv6-stateless"
  }
  
  implicit val circeDecoder: Decoder[Ipv6Mode] = Decoder[String].map {
    case "slaac" => Slaac
    case "dhcpv6-stateful" => Dhcpv6Stateful
    case "dhcpv6-stateless" => Dhcpv6Stateless
  }
  
  case object Slaac extends Ipv6Mode
  case object Dhcpv6Stateful extends Ipv6Mode
  case object Dhcpv6Stateless extends Ipv6Mode
  
  val values: IndexedSeq[Ipv6Mode] = findValues
}
