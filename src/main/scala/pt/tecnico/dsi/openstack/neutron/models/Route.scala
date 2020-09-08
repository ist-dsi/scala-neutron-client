package pt.tecnico.dsi.openstack.neutron.models

import java.net.InetAddress

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Route {
  implicit val codec: Codec[Route] = deriveCodec(renaming.snakeCase)
}

case class Route(destination: String, nexthop: InetAddress)
