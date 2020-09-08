package pt.tecnico.dsi.openstack.neutron.models

import java.net.InetAddress
import java.time.LocalDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object FloatingIp {

  object PortForwarding {
    implicit val decoder: Decoder[PortForwarding] = deriveDecoder(renaming.snakeCase)
  }
  case class PortForwarding(
    protocol: String,
    internalIpAddress: InetAddress,
    internalPort: Int,
    externalPort: Int
  )

  object Read {
    implicit val decoder: Decoder[Read] = deriveDecoder(renaming.snakeCase)
  }
  case class Read(
    id: String,
    routerId: String,
    status: String,
    description: String,
    dnsDomain: String,
    dnsName: String,
    portDetails: String,
    projectId: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    revisionNumber: Int,
    floatingNetworkId: String,
    fixedIpAddress: String,
    floatingIpAddress: String,
    portId: String,
    tags: List[String],
    portForwardings: List[PortForwarding],
    links: List[Link]
  ) extends Identifiable

  object Create {
    implicit val decoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
  case class Create(
    portId: Option[String] = None,
    projectId: Option[String] = None,
    fixedIpAddress: Option[String] = None,
    floatingIpAddress: Option[String] = None,
    description: Option[String] = None,
    subnetId: Option[String] = None,
    dnsDomain: Option[String] = None,
    dnsName: Option[String] = None,
    floatingNetworkId: Option[String] = None
  )

  object Update {
    implicit val decoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  case class Update(
    portId: String,
    fixedIpAddress: Option[String] = None,
    description: Option[String] = None
  )
}

sealed trait FloatingIp extends Model {
  type Update = FloatingIp.Update
  type Create = FloatingIp.Create
  type Read = FloatingIp.Read
}
