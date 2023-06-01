package pt.tecnico.dsi.openstack.neutron.models

import cats.Show
import cats.derived.derived
import io.circe.Codec
import io.circe.derivation.ConfiguredEnumCodec

object Direction:
  given Codec[Direction] = ConfiguredEnumCodec.derive(_.toLowerCase)
enum Direction derives Show:
  case Ingress, Egress
