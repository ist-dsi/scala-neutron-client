package pt.tecnico.dsi.openstack.neutron.models

import cats.Show
import com.comcast.ip4s.{Cidr, Hostname, IpAddress, IpVersion, Ipv4Address, Ipv6Address}
import io.circe.derivation.Configuration
import io.circe.{Codec, Decoder, Encoder}

given Configuration = Configuration.default.withDefaults.withSnakeCaseMemberNames

given Show[IpAddress] = Show.fromToString[IpAddress]

given ipEncoder[IP <: IpAddress]: Encoder[IP] = Encoder[String].contramap(_.toString)
given ipv4Decoder: Decoder[Ipv4Address] = Decoder[String].emap(s => Ipv4Address.fromString(s).toRight(s"Could not parse $s as an IPv4"))
given ipv6Decoder: Decoder[Ipv6Address] = Decoder[String].emap(s => Ipv6Address.fromString(s).toRight(s"Could not parse $s as an IPv6"))
given ipDecoder: Decoder[IpAddress] = Decoder[String].emap(s => IpAddress.fromString(s).toRight(s"Could not parse $s as an IP address"))

given cidrEncoder[IP <: IpAddress]: Encoder[Cidr[IP]] = Encoder[String].contramap(_.toString)
given cidrv4Decoder: Decoder[Cidr[Ipv4Address]] = Decoder[String].emap(s => Cidr.fromString4(s).toRight(s"Could not parse $s as a IPv4 CIDR"))
given cidrv6Decoder: Decoder[Cidr[Ipv6Address]] = Decoder[String].emap(s => Cidr.fromString6(s).toRight(s"Could not parse $s as a IPv6 CIDR"))
given cidrDecoder: Decoder[Cidr[IpAddress]] = Decoder[String].emap(s => Cidr.fromString(s).toRight(s"Could not parse $s as a CIDR"))

given Encoder[Hostname] = Encoder[String].contramap(_.normalized.toString)
given Decoder[Hostname] = Decoder[String].emap(s => Hostname.fromString(s).toRight(s"Could not parse $s as a valid Hostname"))

given Encoder[IpVersion] = Encoder[String].contramap(v => s"IP${v.toString.toLowerCase}")
given Decoder[IpVersion] = Decoder[String].emap:
  case "IPv4" => Right(IpVersion.V4)
  case "IPv6" => Right(IpVersion.V6)
  case s => Left(s"Could not parse $s as a valid IpVersion")
given Show[IpVersion] = Show.fromToString[IpVersion]

val ipVersionIntEncoder: Encoder[IpVersion] = Encoder[Int].contramap:
  case IpVersion.V4 => 4
  case IpVersion.V6 => 6
val ipVersionIntDecoder: Decoder[IpVersion] = Decoder[Int].emap:
  case 4 => Right(IpVersion.V4)
  case 6 => Right(IpVersion.V6)
  case e => Left(s"Invalid IP version: $e")
val ipVersionIntCodec: Codec[IpVersion] = Codec.from(ipVersionIntDecoder, ipVersionIntEncoder)
