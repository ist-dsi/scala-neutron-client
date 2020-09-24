package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object Port {
  object Create {
    implicit val codec: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
  case class Create(
    uplinkStatusPropagation: Option[Boolean] = None,
    macLearningEnabled: Option[Boolean] = None,
    securityGroups: Option[List[String]] = None,
    qosPolicyId: Option[String] = None,
    projectId: Option[String] = None,
    name: Option[String] = None,
    networkId: String,
    portSecurityEnabled: Option[Boolean] = None,
    adminStateUp: Option[Boolean] = None,
    allowedAddressPairs: Option[List[String]] = None,
    bindingHostId: Option[String] = None,
    bindingProfile: Option[Map[String, String]] = None,
    bindingVifType: Option[String] = None,
    deviceId: Option[String] = None,
    deviceOwner: Option[String] = None,
    dnsName: Option[String] = None,
    dnsDomain: Option[String] = None,
    extraDhcpOpts: Option[List[Map[String, String]]] = None,
    fixedIps: Option[List[String]] = None,
    macAddress: Option[String] = None,
  )

  object Update {
    implicit val codec: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  case class Update(
    name: Option[String] = None,
    adminStateUp: Option[Boolean] = None,
    allowedAddressPairs: Option[List[String]] = None,
    bindingHostId: Option[String] = None,
    bindingProfile: Option[Map[String, String]] = None,
    bindingVnicType: Option[String] = None,
    dataPlaneStatus: Option[String] = None,
    deviceId: Option[String] = None,
    description: Option[String] = None,
    deviceOwner: Option[String] = None,
    dnsName: Option[String] = None,
    dnsDomain: Option[String] = None,
    extraDhcpOpts: Option[List[Map[String, String]]] = None,
    fixedIps: Option[List[String]] = None,
    macAddress: Option[String] = None,
    securityGroups: Option[List[String]] = None,
    qosPolicyId: Option[String] = None,
    macLearningEnabled: Option[Boolean] = None,
  )
  
  implicit val decoder: Decoder[Port] = withRenames(deriveDecoder[Port](renaming.snakeCase))(
    "binding:profile" -> "binding_profile",
    "binding:vif_details" -> "binding_vif_details",
    "binding:vif_type" -> "binding_vif_type",
    "binding:host_id" -> "binding_host_id",
    "binding:vnic_type" -> "binding_vnic_type"
  )
}
case class Port(
  id: String,
  name: String,
  description: String,
  projectId: String,
  
  adminStateUp: Boolean,
  status: String,
  networkId: String,
  allowedAddressPairs: List[String],
  bindingHostId: Option[String],
  bindingProfile: Map[String, String],
  // bindingVifDetails: Map[String, Any], ?? (investigate)
  bindingVifType: String,
  bindingVnicType: String,
  dataPlaneStatus: Option[String],
  deviceId: String,
  deviceOwner: String,
  dnsAssignment: List[Map[String, String]],
  dnsDomain: Option[String],
  dnsName: String,
  extraDhcpOpts: List[Map[String, String]],
  //  fixedIps: List[String],
  ipAllocation: Option[String],
  macAddress: String,
  portSecurityEnabled: Boolean,
  qosNetworkPolicyId: Option[String],
  qosPolicyId: Option[String],
  resourceRequest: Option[Map[String, String]],
  securityGroups: List[String],
  uplinkStatusPropagation: Option[Boolean],
  macLearningEnabled: Option[Boolean],
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String] = List.empty,
  links: List[Link] = List.empty
) extends Identifiable