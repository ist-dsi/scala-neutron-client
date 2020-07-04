package pt.tecnico.dsi.neutron.models

import java.time.LocalDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object FloatingIp {

  object Read {
    implicit val decoder: Decoder[Read] = deriveDecoder(renaming.snakeCase)
  }

  object Update {
    implicit val decoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }

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

  case class Update(
    portId: String,
    fixedIpAddress: Option[String] = None,
    description: Option[String] = None
  )

  case class Read(
    routerId: String,
    status: String,
    description: String,
    dnsDomain: String,
    dnsName: String,
    portDetails: String,
    projectId: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    revisionNumber: Integer,
    floatingNetworkId: String,
    fixedIpAddress: String,
    floatingIpAddress: String,
    portId: String,
    tags: List[String],
    portForwardings: List[Map[String, String]]
  )

}

sealed trait FloatingIp extends Model {
  type Update = FloatingIp.Update
  type Create = FloatingIp.Create
  type Read = FloatingIp.Read
}
