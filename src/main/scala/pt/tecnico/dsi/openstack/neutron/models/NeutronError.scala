package pt.tecnico.dsi.openstack.neutron.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder,renaming}

object NeutronError {
  implicit val decoder: Decoder[NeutronError] = deriveDecoder[NeutronError](renaming.snakeCase).at("NeutronError")
}
case class NeutronError(`type`: String, message: String, detail: String) extends Exception(s"$message\n$detail")
