package pt.tecnico.dsi.neutron.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object RouterInterface {
  implicit val codec: Decoder[RouterInterface] = deriveDecoder(renaming.snakeCase)

  object Remove {
    implicit val codec: Encoder[Remove] = deriveEncoder(renaming.snakeCase)
  }

  case class Remove (
    subnetId: Option[String],
    portId: Option[String],
  )
}

case class RouterInterface(
  subnetId: String,
  projectId: String,
  portId: String,
  networkId: String,
  tags: List[String]
)