package pt.tecnico.dsi.neutron

import java.net.InetAddress

import io.circe.{Decoder, Encoder}

package object models {
  implicit val ipv4Decoder: Decoder[InetAddress] = Decoder.decodeString.map(InetAddress.getByName)
  implicit val ipv4Encoder: Encoder[InetAddress] = Encoder.encodeString.contramap(_.getHostAddress)
}
