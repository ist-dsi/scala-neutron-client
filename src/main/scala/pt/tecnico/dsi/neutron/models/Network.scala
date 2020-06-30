package pt.tecnico.dsi.neutron.models

import java.time.LocalDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder, HCursor}

object Network {

  // TODO: Rethink this
  implicit val decoder: Decoder[Read] = (c: HCursor) => {
    for {
      providerNetworkType      <- c.downField("provider:network_type").as[String]
      providerPhysicalNetwork  <- c.downField("provider:physical_network").as[String]
      providerSegmentationId   <- c.downField("provider:segmentation_id").as[Integer]
      routerExternal           <- c.downField("router:external").as[Boolean]
      network                  <- c.as[Read](deriveDecoder(renaming.snakeCase))
    } yield network.copy(
      providerNetworkType = providerNetworkType,
      providerPhysicalNetwork = providerPhysicalNetwork,
      providerSegmentationId = providerSegmentationId,
      routerExternal = routerExternal
    )
  }

  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }

  case class Create(
    adminStateUp: Option[Boolean],
    dnsDomain: Option[String],
    mtu: Option[Integer],
    name: Option[String],
    portSecurityEnabled: Option[Boolean],
    projectId: Option[String],
    providerNetworkType: Option[String],
    providerPhysicalNetwork: Option[String],
    providerSegmentationId: Option[Integer],
    qosPolicyId: Option[String],
    routerExternal: Option[Boolean],
    tenantId: Option[String],
    isDefault: Option[Boolean],
    availabilityZoneHints: Option[Seq[String]],
    segments: Option[Seq[String]],
    shared: Option[Boolean],
    vlanTransparent: Option[Boolean],
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }

  sealed case class Update(
    adminStateUp: Option[Boolean],
    dnsDomain: Option[String],
    mtu: Option[Integer],
    name: Option[String],
    portSecurityEnabled: Option[Boolean],
    projectId: Option[String],
    providerNetworkType: Option[String],
    providerPhysicalNetwork: Option[String],
    providerSegmentationId: Option[Integer],
    qosPolicyId: Option[String],
    routerExternal: Option[Boolean],
    isDefault: Option[Boolean],
    segments: Option[Seq[String]],
    shared: Option[Boolean],
  )

  sealed case class Read(
    adminStateUp: Boolean,
    availabilityZoneHints: Seq[String], // ???
    availabilityZones: Seq[String], // ???
    createdAt: LocalDateTime,
    dnsDomain: String,
    ipv4AddressScope: String,
    ipv6AddressScope: String,
    l2Adjacency: Boolean,
    mtu: Integer,
    name: String,
    portSecurityEnabled: Boolean,
    projectId: String,
    // provider:network_type
    providerNetworkType: String,
    providerPhysicalNetwork: String,
    providerSegmentationId: Integer,
    qosPolicyId: String,
    revision_number: Integer,
    // router:external
    routerExternal: Boolean,
    segments: Seq[String], // ???
    shared: Boolean,
    subnets: Seq[String], //???
    tenantId: String,
    updatedAt: LocalDateTime,
    vlanTransparent: Boolean,
    description: String,
    isDefault: Boolean,
    tags: Seq[String],
  )

}

sealed trait Network extends Model {
  type Update = Network.Update
  type Create = Network.Create
  type Read = Network.Read

  implicit val e: Encoder[Network.Create] = deriveEncoder(renaming.snakeCase)
  implicit val g: Encoder[Network.Update] = deriveEncoder(renaming.snakeCase)
  implicit val f: Decoder[Network.Read] = deriveDecoder(renaming.snakeCase)
}
