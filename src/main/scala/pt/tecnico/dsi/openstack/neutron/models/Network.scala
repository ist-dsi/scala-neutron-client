package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object Network {
  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
  case class Create(
    projectId: Option[String] = None,
    name: String,
    description: String = "",
    adminStateUp: Option[Boolean] = None,
    dnsDomain: Option[String] = None,
    mtu: Option[Int] = None,
    portSecurityEnabled: Option[Boolean] = None,
    providerNetworkType: Option[String] = None,
    providerPhysicalNetwork: Option[String] = None,
    providerSegmentationId: Option[Int] = None,
    routerExternal: Option[Boolean] = None,
    isDefault: Option[Boolean] = None,
    availabilityZoneHints: Option[List[String]] = None,
    shared: Option[Boolean] = None,
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  sealed case class Update(
    adminStateUp: Option[Boolean] = None,
    dnsDomain: Option[String] = None,
    mtu: Option[Int] = None,
    name: Option[String] = None,
    portSecurityEnabled: Option[Boolean] = None,
    projectId: Option[String] = None,
    providerNetworkType: Option[String] = None,
    providerPhysicalNetwork: Option[String] = None,
    providerSegmentationId: Option[Int] = None,
    routerExternal: Option[Boolean] = None,
    isDefault: Option[Boolean] = None,
    shared: Option[Boolean] = None,
  )
  
  implicit val decoder: Decoder[Network] = withRenames(deriveDecoder[Network](renaming.snakeCase))(
    "provider:network_type" -> "type",
    "provider:physical_network" -> "physical_network",
    "provider:segmentation_id" -> "segmentation_id",
    "router:external" -> "router_external",
    "revision_number" -> "revision"
  )
}

sealed case class Network(
  id: String,
  name: String,
  description: String,
  projectId: String,
  
  adminStateUp: Boolean,
  status: String, // ACTIVE, DOWN, BUILD or ERROR.
  `type`: String,
  mtu: Int,
  dnsDomain: String, // Cannot be ip4s Hostname because it ends with '.'
  ipv4AddressScope: Option[String],
  ipv6AddressScope: Option[String],
  portSecurityEnabled: Boolean,
  physicalNetwork: Option[String],
  segmentationId: Int,
  routerExternal: Boolean,
  shared: Boolean,
  //subnets: List[String], // List of subnet ids
  isDefault: Boolean = false, // missing also
  availabilityZoneHints: List[String] = List.empty, // Probably is not String
  availabilityZones: List[String] = List.empty,
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String] = List.empty,
  links: List[Link] = List.empty
) extends Identifiable