package pt.tecnico.dsi.openstack.neutron

import cats.Show
import com.comcast.ip4s.{Cidr, Hostname, IpAddress, IpVersion, Ipv4Address, Ipv6Address}
import io.circe.{Codec, Decoder, Encoder}

package object models {
  implicit def ipEncoder[IP <: IpAddress]: Encoder[IP] = Encoder[String].contramap(_.toString)
  implicit val ipv4Decoder: Decoder[Ipv4Address] = Decoder[String].emap(s => Ipv4Address(s).toRight(s"Could not parse $s as an IPv4"))
  implicit val ipv6Decoder: Decoder[Ipv6Address] = Decoder[String].emap(s => Ipv6Address(s).toRight(s"Could not parse $s as an IPv6"))
  implicit val ipDecoder: Decoder[IpAddress] = Decoder[String].emap(s => IpAddress(s).toRight(s"Could not parse $s as an IP address"))
  
  implicit def cidrEncoder[IP <: IpAddress]: Encoder[Cidr[IP]] = Encoder[String].contramap(_.toString)
  implicit val cidrv4Decoder: Decoder[Cidr[Ipv4Address]] = Decoder[String].emap(s => Cidr.fromString4(s).toRight(s"Could not parse $s as a IPv4 CIDR"))
  implicit val cidrv6Decoder: Decoder[Cidr[Ipv6Address]] = Decoder[String].emap(s => Cidr.fromString6(s).toRight(s"Could not parse $s as a IPv6 CIDR"))
  implicit val cidrDecoder: Decoder[Cidr[IpAddress]] = Decoder[String].emap(s => Cidr.fromString(s).toRight(s"Could not parse $s as a CIDR"))
  
  implicit val hostnameEncoder: Encoder[Hostname] = Encoder[String].contramap(_.normalized.toString)
  implicit val hostnameDecoder: Decoder[Hostname] = Decoder[String].emap(s => Hostname(s).toRight(s"Could not parse $s as a valid Hostname"))
  
  implicit val ipVersionEncoder: Encoder[IpVersion] = Encoder[String].contramap(v => s"IP${v.toString.toLowerCase}")
  implicit val ipVersionDecoder: Decoder[IpVersion] = Decoder[String].emap {
    case "IPv4" => Right(IpVersion.V4)
    case "IPv6" => Right(IpVersion.V6)
    case s => Left(s"Could not parse $s as a valid IpVersion")
  }
  implicit val ipVersionShow: Show[IpVersion] = Show.fromToString[IpVersion]
  
  val ipVersionIntEncoder: Encoder[IpVersion] = Encoder[Int].contramap {
    case IpVersion.V4 => 4
    case IpVersion.V6 => 6
  }
  val ipVersionIntDecoder: Decoder[IpVersion] = Decoder[Int].emap {
    case 4 => Right(IpVersion.V4)
    case 6 => Right(IpVersion.V6)
    case e => Left(s"Invalid IP version: $e")
  }
  val ipVersionIntCodec: Codec[IpVersion] = Codec.from(ipVersionIntDecoder, ipVersionIntEncoder)
}
