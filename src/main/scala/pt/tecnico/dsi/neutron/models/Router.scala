package pt.tecnico.dsi.neutron.models

import java.time.LocalDateTime

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming, deriveCodec}

object Router extends Model {

  object Read {
    implicit val codec: Decoder[Read] = deriveDecoder(renaming.snakeCase)
  }

  object Create {
    implicit val codec: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }

  object Update {
    implicit val codec: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }

  object ConntrackHelper {
    implicit val decoder: Decoder[ConntrackHelper] = deriveDecoder(renaming.snakeCase)
  }

  case class ConntrackHelper(protocol: String, helper: String, port: Integer)

  case class Read(
    projectId: String,
    name: String,
    description: String,
    adminStateUp: Boolean,
    status: String, // ???
    externalGatewayInfo: Map[String, String], // ???
    revisionNumber: Integer,
    routes: List[Map[String, String]], // ???
    destination: String,
    nexthop: String, // Ipv4Address
    distributed: Boolean,
    ha: Boolean,
    availabilityZoneHints: List[String], //??
    availabilityZones: List[String], //???
    serviceTypeId: String, //???
    flavorId: String, //???
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    tags: List[String],
    conntrack_helpers: List[ConntrackHelper]

  )

  case class Create(
    projectId: Option[String],
    name: Option[String],
    description: Option[String],
    adminStateUp: Option[Boolean],
    externalGatewayInfo: Option[Map[String, String]],
    distributed: Option[Boolean],
    ha: Option[Boolean],
    availabilityZoneHints: Option[List[String]],
    serviceTypeId: Option[String],
    flavorId: Option[String],
  )

  case class Update(
    name: Option[String],
    description: Option[String],
    adminStateUp: Option[Boolean],
    externalGatewayInfo: Option[Map[String, String]],
    distributed: Option[Boolean],
    ha: Option[Boolean],
    routes: List[Map[String, String]],
  )
}

sealed trait Router extends Model {
  type Update = Router.Update
  type Create = Router.Create
  type Read = Router.Read
}
