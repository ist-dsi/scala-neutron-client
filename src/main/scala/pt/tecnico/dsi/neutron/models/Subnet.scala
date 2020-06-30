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
    name: Option[String] = None,
    enableDhcp: Option[Boolean] = None,
    dnsNameservers: Option[List[String]] = None, // ???
    allocationPools: Option[List[Map[String, String]]] = None, // ???
    hostRoutes: Option[List[Map[String, String]]] = None, // ???
    gatewayIp: Option[String] = None,
    description: Option[String] = None,
    serviceTypes: Option[List[String]] = None,
    segmentId: Option[String] = None,
    dnsPublishFixedIp: Option[Boolean] = None
  )

  case class Create(
    projectId: Option[String] = None,
    name: Option[String] = None,
    enableDhcp: Option[Boolean] = None,
    networkId: String,
    dnsNameservers: List[String] = List.empty, // ???
    allocationPools: Option[List[Map[String, String]]] = None, // ???
    hostRoutes: Option[List[Map[String, String]]] = None, // ???
    ipVersion: Integer,
    gatewayIp: Option[String] = None,
    cidr: String,
    prefixlen: Option[Integer] = None,
    description: Option[String] = None,
    ipv6AddressMode: Option[Ipv6Mode] = None,
    ipv6RaMode: Option[Ipv6Mode] = None,
    segmentId: Option[String] = None,
    subnetpoolId: Option[String] = None,
    useDefaultSubnetpool: Option[Boolean] = None,
    serviceTypes: List[String] = Nil,
    dnsPublishFixedIp: Option[Boolean] = None
  )

  case class Read(
    name: String,
    enableDhcp: Boolean,
    networkId: String,
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
    allocationPools: List[Map[String, String]],
    description: String,
    ipv6AddressMode: Option[Ipv6Mode],
    ipv6RaMode: Option[Ipv6Mode],
    revisionNumber: Integer,
    segmentId: String,
    subnetpoolId: String,
    updatedAt: LocalDateTime,
    tags: List[String],
    dnsPublishFixedIp: Boolean,
    //serviceTypes: List[???],
  )
}

sealed trait Subnet extends Model {
  type Update = Subnet.Update
  type Create = Subnet.Create
  type Read = Subnet.Read
}
