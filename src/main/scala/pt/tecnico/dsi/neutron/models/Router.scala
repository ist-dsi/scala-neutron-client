package pt.tecnico.dsi.neutron.models

import java.net.InetAddress
import java.time.LocalDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Router extends Model {

  object Read {
    implicit val codec: Decoder[Read] = deriveDecoder(renaming.snakeCase)
  }

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
    nexthop: InetAddress,
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

  object Create {
    implicit val codec: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }

  case class Create(
    projectId: Option[String] = None,
    name: Option[String] = None,
    description: Option[String] = None,
    adminStateUp: Option[Boolean] = None,
    externalGatewayInfo: Option[Map[String, String]] = None,
    distributed: Option[Boolean] = None,
    ha: Option[Boolean] = None,
    availabilityZoneHints: Option[List[String]] = None,
    serviceTypeId: Option[String] = None,
    flavorId: Option[String] = None,
  )

  object Update {
    implicit val codec: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }

  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
    adminStateUp: Option[Boolean] = None,
    externalGatewayInfo: Option[Map[String, String]] = None,
    distributed: Option[Boolean] = None,
    ha: Option[Boolean] = None,
    routes: Option[List[Map[String, String]]] = None,
  )

  object ConntrackHelper {
    implicit val decoder: Decoder[ConntrackHelper] = deriveDecoder(renaming.snakeCase)
  }

  case class ConntrackHelper(protocol: String, helper: String, port: Integer)
}

sealed trait Router extends Model {
  type Update = Router.Update
  type Create = Router.Create
  type Read = Router.Read
}
