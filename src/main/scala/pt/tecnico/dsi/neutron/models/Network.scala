package pt.tecnico.dsi.neutron.models

import java.time.ZonedDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object Network {

  object Read {
    implicit val decoder: Decoder[Read] = decoderAfterRename[Read](
      Map(
        "provider:network_type" -> "provider_network_type",
        "provider:physical_network" -> "provider_physical_network",
        "provider:segmentation_id" -> "provider_segmentation_id",
        "router:external" -> "router_external",
      ), deriveDecoder(renaming.snakeCase))
  }

  sealed case class Read(
    id: String,
    adminStateUp: Boolean,
    availabilityZoneHints: List[String], // ???
    availabilityZones: List[String], // ???
    createdAt: ZonedDateTime,
    dnsDomain: String,
    ipv4AddressScope: Option[String],
    ipv6AddressScope: Option[String],
    l2Adjacency: Option[Boolean],
    mtu: Integer,
    name: String,
    portSecurityEnabled: Boolean,
    projectId: String,
    // provider:network_type
    providerNetworkType: String,
    providerPhysicalNetwork: Option[String],
    providerSegmentationId: Integer,
    qosPolicyId: Option[String],
    revision_number: Integer,
    // router:external
    routerExternal: Boolean,
    segments: List[String] = List.empty, // ???
    shared: Boolean,
    subnets: List[String], //???
    updatedAt: ZonedDateTime,
    vlanTransparent: Boolean = false, // Where is this?
    description: String,
    isDefault: Boolean = false, // missing also
    tags: List[String],
    links: List[Link] = List.empty
  ) extends Identifiable

  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }

  case class Create(
    adminStateUp: Option[Boolean] = None,
    dnsDomain: Option[String] = None,
    mtu: Option[Integer] = None,
    name: Option[String] = None,
    portSecurityEnabled: Option[Boolean] = None,
    projectId: Option[String] = None,
    providerNetworkType: Option[String] = None,
    providerPhysicalNetwork: Option[String] = None,
    providerSegmentationId: Option[Integer] = None,
    qosPolicyId: Option[String] = None,
    routerExternal: Option[Boolean] = None,
    isDefault: Option[Boolean] = None,
    availabilityZoneHints: Option[List[String]] = None,
    segments: Option[List[String]] = None,
    shared: Option[Boolean] = None,
    vlanTransparent: Option[Boolean] = None,
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }

  sealed case class Update(
    adminStateUp: Option[Boolean] = None,
    dnsDomain: Option[String] = None,
    mtu: Option[Integer] = None,
    name: Option[String] = None,
    portSecurityEnabled: Option[Boolean] = None,
    projectId: Option[String] = None,
    providerNetworkType: Option[String] = None,
    providerPhysicalNetwork: Option[String] = None,
    providerSegmentationId: Option[Integer] = None,
    qosPolicyId: Option[String] = None,
    routerExternal: Option[Boolean] = None,
    isDefault: Option[Boolean] = None,
    segments: Option[List[String]] = None,
    shared: Option[Boolean] = None,
  )

}

sealed trait Network extends Model {
  type Update = Network.Update
  type Create = Network.Create
  type Read = Network.Read
}
