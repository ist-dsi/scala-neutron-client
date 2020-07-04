package pt.tecnico.dsi.neutron.models

import io.circe.{Codec, Decoder, Encoder}
import io.circe.derivation.{deriveEncoder, deriveDecoder, renaming}

object Route {
  implicit val encoder: Encoder[Route] = deriveEncoder(renaming.snakeCase)
  implicit val decoder: Decoder[Route] = deriveDecoder(renaming.snakeCase)
}

case class Route(destination: String, nexthop: String)
