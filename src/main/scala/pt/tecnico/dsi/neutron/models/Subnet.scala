package pt.tecnico.dsi.neutron.models

import java.time.LocalDateTime

import enumeratum.{Enum, EnumEntry}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Subnet {

  // For lack of a better name
  sealed trait Ipv6Mode extends EnumEntry
  case object Ipv6Mode extends Enum[Ipv6Mode] {

    implicit val circeEncoder: Encoder[Ipv6Mode] = Encoder.encodeString.contramap {
      case Slaac => "slaac"
      case Dhcpv6Stateful => "dhcpv6-stateful"
      case Dhcpv6Stateless => "dhcpv6-stateless"
    }

    implicit val circeDecoder: Decoder[Ipv6Mode] = Decoder.decodeString.map {
      case "slaac" => Slaac
      case "dhcpv6-stateful" => Dhcpv6Stateful
      case "dhcpv6-stateless" => Dhcpv6Stateless
    }

    case object Slaac extends Ipv6Mode
    case object Dhcpv6Stateful extends Ipv6Mode
    case object Dhcpv6Stateless extends Ipv6Mode

    val values: IndexedSeq[Ipv6Mode] = findValues
  }

  object Read {
    implicit val decoder: Decoder[Read] = deriveDecoder(renaming.snakeCase)
  }

  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }

  case class Update(
    name: Option[String],
    enableDhcp: Option[Boolean],
    dnsNameservers: Option[Seq[String]], // ???
    allocationPools: Option[Seq[Map[String, String]]], // ???
    hostRoutes: Option[Seq[Map[String, String]]], // ???
    gatewayIp: Option[String],
    description: Option[String],
    serviceTypes: Option[Seq[String]],
    segmentId: Option[String],
    dnsPublishFixedIp: Option[Boolean]
  )

  case class Create(
    tenantId: Option[String],
    projectId: Option[String],
    name: Option[String],
    enableDhcp: Option[Boolean],
    networkId: String,
    dnsNameservers: Seq[String], // ???
    allocationPools: Option[Seq[Map[String, String]]], // ???
    hostRoutes: Option[Seq[Map[String, String]]], // ???
    ipVersion: Integer,
    gatewayIp: Option[String],
    cidr: String,
    prefixlen: Option[Integer],
    description: Option[String],
    ipv6AddressMode: Option[Ipv6Mode],
    ipv6RaMode: Option[Ipv6Mode],
    segmentId: Option[String],
    subnetpoolId: Option[String],
    useDefaultSubnetpool: Option[Boolean],
    serviceTypes: Seq[String],
    dnsPublishFixedIp: Option[Boolean]
  )

  case class Read(
    name: String,
    enableDhcp: Boolean,
    networkId: String,
    tenantId: String,
    projectId: String,
    ipVersion: Integer,
    gatewayIp: String,
    cidr: String,
    createdAt: LocalDateTime,
    /*
    "allocation_pools": [
      {
        "start": "10.0.0.2",
        "end": "10.0.0.254"
      }
    ],
    */
    allocationPools: Seq[Map[String, String]],
    description: String,
    ipv6AddressMode: Option[Ipv6Mode],
    ipv6RaMode: Option[Ipv6Mode],
    revisionNumber: Integer,
    segmentId: String,
    subnetpoolId: String,
    updatedAt: LocalDateTime,
    tags: Seq[String],
    dnsPublishFixedIp: Boolean,
    //serviceTypes: Seq[???],
  )
}

sealed trait Subnet extends Model {
  type Update = Subnet.Update
  type Create = Subnet.Create
  type Read = Subnet.Read

  implicit val e: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  implicit val g: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  implicit val f: Decoder[Read] = deriveDecoder(renaming.snakeCase)
}
