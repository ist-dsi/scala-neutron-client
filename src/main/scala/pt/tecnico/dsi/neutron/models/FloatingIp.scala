package pt.tecnico.dsi.neutron.models

import java.time.LocalDateTime

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, renaming, deriveEncoder}

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
    portId: Option[String],
    projectId: Option[String],
    fixedIpAddress: Option[String],
    floatingIpAddress: Option[String],
    description: Option[String],
    subnetId: Option[String],
    dnsDomain: Option[String],
    dnsName: Option[String],
    floatingNetworkId: Option[String]
  )

  case class Update(
    portId: String,
    fixedIpAddress: Option[String],
    description: Option[String]
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
