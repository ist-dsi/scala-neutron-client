package pt.tecnico.dsi.openstack.neutron.models

import enumeratum.{Circe, Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

sealed trait Direction extends EnumEntry
case object Direction extends Enum[Direction] {
  implicit val circeEncoder: Encoder[Direction] = Circe.encoderLowercase(this)
  implicit val circeDecoder: Decoder[Direction] = Circe.decoderLowercaseOnly(this)
  
  case object Ingress extends Direction
  case object Egress extends Direction
  
  val values: IndexedSeq[Direction] = findValues
}