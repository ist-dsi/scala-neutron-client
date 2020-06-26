package pt.tecnico.dsi.neutron

import io.circe.{Decoder, Encoder}
import squants.information.Information
import squants.information.InformationConversions._

package object models {
  implicit val decoderInformation: Decoder[Information] = Decoder.decodeInt.map(_.gibibytes)
  implicit val encoderInformation: Encoder[Information] = Encoder.encodeInt.contramap(_.toGibibytes.ceil.toInt)
}
