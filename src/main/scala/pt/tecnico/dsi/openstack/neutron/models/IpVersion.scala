package pt.tecnico.dsi.openstack.neutron.models

import enumeratum.{CirceEnum, Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

// TODO: No longer needed when https://github.com/Comcast/ip4s/pull/177 is released
// the encoder/decoder will still be needed
sealed trait IpVersion extends EnumEntry
case object IpVersion extends Enum[IpVersion] with CirceEnum[IpVersion] { self =>
  val intEncoder: Encoder[IpVersion] = Encoder[Int].contramap {
    case IpVersion.IPv4 => 4
    case IpVersion.IPv6 => 6
  }
  val intDecoder: Decoder[IpVersion] = Decoder[Int].emap {
    case 4 => Right(IpVersion.IPv4)
    case 6 => Right(IpVersion.IPv6)
    case e => Left(s"Invalid IP version: $e")
  }
  
  case object IPv4 extends IpVersion
  case object IPv6 extends IpVersion
  val values: IndexedSeq[IpVersion] = findValues
}
