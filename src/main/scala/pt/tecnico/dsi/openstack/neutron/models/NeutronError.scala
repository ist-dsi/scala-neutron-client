package pt.tecnico.dsi.openstack.neutron.models

import io.circe.Decoder
import io.circe.derivation.ConfiguredDecoder

object NeutronError:
  given Decoder[NeutronError] = ConfiguredDecoder.derived[NeutronError].at("NeutronError")
case class NeutronError(`type`: String, message: String, detail: String = "") extends Exception(s"$message\n$detail")
