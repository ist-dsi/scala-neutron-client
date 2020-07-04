package pt.tecnico.dsi.neutron

import java.net.Inet4Address
import java.net.InetAddress

import io.circe.{Decoder, Encoder}
import squants.information.Information
import squants.information.InformationConversions._

package object models {
  implicit val decoderInformation: Decoder[Information] = Decoder.decodeInt.map(_.gibibytes)
  implicit val encoderInformation: Encoder[Information] = Encoder.encodeInt.contramap(_.toGibibytes.ceil.toInt)

  implicit val ipv4Decoder: Decoder[InetAddress] = Decoder.decodeString.map(InetAddress.getByName)
  implicit val ipv4Encoder: Encoder[InetAddress] = Encoder.encodeString.contramap(_.getHostAddress)
}
