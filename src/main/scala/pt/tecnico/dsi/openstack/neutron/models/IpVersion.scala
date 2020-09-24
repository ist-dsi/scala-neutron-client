package pt.tecnico.dsi.openstack.neutron.models

import enumeratum.{Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

// TODO: No longer needed when https://github.com/Comcast/ip4s/pull/177 is released
// the encoder/decoder will still be needed
sealed trait IpVersion extends EnumEntry
case object IpVersion extends Enum[IpVersion] { self =>
  implicit val circeEncoder: Encoder[IpVersion] = Encoder[String].contramap(version => s"IP${version.entryName.toLowerCase}")
  implicit val circeDecoder: Decoder[IpVersion] = Decoder[String].emap {
    case "IPv4" => Right(IpVersion.V4)
    case "IPv6" => Right(IpVersion.V6)
    case s => Left(s"'$s' is not a member of enum $self")
  }
  
  case object V4 extends IpVersion
  case object V6 extends IpVersion
  val values: IndexedSeq[IpVersion] = findValues
}
