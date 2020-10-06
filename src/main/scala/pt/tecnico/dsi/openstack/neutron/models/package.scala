package pt.tecnico.dsi.openstack.neutron

import com.comcast.ip4s.{Cidr, Hostname, IpAddress, Ipv4Address, Ipv6Address}
import io.circe.{Decoder, Encoder}

package object models {
  implicit def ipEncoder[IP <: IpAddress]: Encoder[IP] = Encoder[String].contramap(_.toString)
  implicit val ipv4Decoder: Decoder[Ipv4Address] = Decoder[String].emap(s => Ipv4Address(s).toRight(s"Could not parse $s as an IPv4"))
  implicit val ipv6Decoder: Decoder[Ipv6Address] = Decoder[String].emap(s => Ipv6Address(s).toRight(s"Could not parse $s as an IPv6"))
  implicit val ipDecoder: Decoder[IpAddress] = Decoder[String].emap(s => IpAddress(s).toRight(s"Could not parse $s as an IP address"))
  //implicit def ipDecoder[IP <: IpAddress]: Decoder[IP] = ???
  
  implicit def cidrEncoder[IP <: IpAddress]: Encoder[Cidr[IP]] = Encoder[String].contramap(_.toString)
  implicit val cidrv4Decoder: Decoder[Cidr[Ipv4Address]] = Decoder[String].emap(s => Cidr.fromString4(s).toRight(s"Could not parse $s as a IPv4 CIDR"))
  implicit val cidrv6Decoder: Decoder[Cidr[Ipv6Address]] = Decoder[String].emap(s => Cidr.fromString6(s).toRight(s"Could not parse $s as a IPv6 CIDR"))
  implicit val cidrDecoder: Decoder[Cidr[IpAddress]] = Decoder[String].emap(s => Cidr.fromString(s).toRight(s"Could not parse $s as a CIDR"))
  //implicit def cidrDecoder[IP <: IpAddress]: Decoder[Cidr[IP]] = ???
  
  implicit val hostnameEncoder: Encoder[Hostname] = Encoder[String].contramap(_.normalized.toString)
  implicit val hostnameDecoder: Decoder[Hostname] = Decoder[String].emap(s => Hostname(s).toRight(s"Could not parse $s as a valid Hostname"))
  
  // TODO: No longer needed when https://github.com/Comcast/ip4s/pull/177 is released
  implicit class RichIp(ip: IpAddress) {
    def version: IpVersion = ip.fold(_ => IpVersion.IPv4, _ => IpVersion.IPv6)
  }
  
  // https://github.com/Comcast/ip4s/issues/187
  implicit class RichCidr[+A <: IpAddress](val cidr: Cidr[A]) extends AnyVal {
    def totalIps: BigInt = {
      import scala.math._
      val maxPrefixBits: Double = cidr.address.fold(_ => 32.0, _ => 128.0)
      val ips = pow(2d, maxPrefixBits - cidr.prefixBits)
      BigDecimal(ips).setScale(0, BigDecimal.RoundingMode.HALF_UP).toBigInt
    }
  }
}
