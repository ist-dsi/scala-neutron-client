package pt.tecnico.dsi.openstack.neutron.models

import cats.Show
import cats.derived.derived
import io.circe.derivation.{Configuration, ConfiguredEnumCodec}
import io.circe.Codec

object Ipv6Mode:
  given Codec[Ipv6Mode] = ConfiguredEnumCodec.derive:
    case "Dhcpv6Stateful" => "dhcpv6-stateful"
    case "Dhcpv6Stateless" => "dhcpv6-stateless"
    case s => s.toLowerCase
enum Ipv6Mode derives Show:
  case Slaac, Dhcpv6Stateful, Dhcpv6Stateless
