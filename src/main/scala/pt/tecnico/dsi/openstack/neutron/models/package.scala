package pt.tecnico.dsi.openstack.neutron

import com.comcast.ip4s.{Cidr, Hostname, IpAddress, Ipv4Address, Ipv6Address}
import io.circe.{Decoder, Encoder, JsonObject}

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
  implicit class ipextensions(ip: IpAddress) {
    def version: IpVersion = ip.fold(_ => IpVersion.IPv4, _ => IpVersion.IPv6)
  }
  
  private def renameJsonObjectFields(renames: Seq[(String, String)])(obj: JsonObject): JsonObject = {
    val renamesMap = renames.toMap
    val newMap = obj.toMap.map { case (key, value) =>
      renamesMap.getOrElse(key, key) -> value
    }
    JsonObject.fromMap(newMap)
  }
  
  // https://github.com/circe/circe-derivation/issues/245
  /** Creates a decoder from `initial` that performs the `renames` to the JsonObject keys before decoding. */
  def withRenames[T](initial: Decoder[T])(renames: (String, String)*): Decoder[T] = initial.prepare {
    _.withFocus {
      _.mapObject(renameJsonObjectFields(renames))
    }
  }
  
  def withRenames[T](initial: Encoder.AsObject[T])(renames: (String, String)*): Encoder.AsObject[T] =
    initial.mapJsonObject(renameJsonObjectFields(renames))
}
