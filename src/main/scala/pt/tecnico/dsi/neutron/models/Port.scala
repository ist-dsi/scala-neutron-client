package pt.tecnico.dsi.neutron.models

import java.time.LocalDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Port {
  implicit val codec: Decoder[Port] = deriveDecoder(renaming.snakeCase)
}

object PortCreate {
  implicit val codec: Encoder[PortCreate] = deriveEncoder(renaming.snakeCase)
}

object PortUpdate {
  implicit val codec: Encoder[PortUpdate] = deriveEncoder(renaming.snakeCase)
}

case class PortUpdate(
  name: Option[String],
  adminStateUp: Option[Boolean],
  allowedAddressPairs: Option[Seq[String]],
  bindingHostId: Option[String],
  bindingProfile: Option[Map[String,String]],
  bindingVnicType: Option[String],
  dataPlaneStatus: Option[String],
  deviceId: Option[String],
  description: Option[String],
  deviceOwner: Option[String],
  dnsName: Option[String],
  dnsDomain: Option[String],
  extraDhcpOpts: Option[Seq[Map[String,String]]],
  fixedIps: Option[Seq[String]],
  macAddress: Option[String],
  securityGrous: Option[Seq[String]],
  qosPolicyId: Option[String],
  macLearningEnabled: Option[Boolean],
)

case class PortCreate(
  uplinkStatusPropagation: Option[Boolean],
  macLearningEnabled: Option[Boolean],
  tenantId: Option[String],
  securityGrous: Option[Seq[String]],
  qosPolicyId: Option[String],
  projectId: Option[String],
  name: Option[String],
  networkId: Option[String],
  portSecurityEnabled: Option[Boolean],
  adminStateUp: Option[Boolean],
  allowedAddressPairs: Option[Seq[String]],
  bindingHostId: Option[String],
  bindingProfile: Option[Map[String,String]],
  bindingVifType: Option[String],
  deviceId: Option[String],
  deviceOwner: Option[String],
  dnsName: Option[String],
  dnsDomain: Option[String],
  extraDhcpOpts: Option[Seq[Map[String,String]]],
  fixedIps: Option[Seq[String]],
  macAddress: Option[String],
)

case class Port(
  adminStateUp: Boolean,
  allowedAddressPairs: Seq[String],
  bindingHostId: String,
  bindingProfile: Map[String, String],
  bindingVifDetails: Map[String, String],
  bindingVifType: String,
  bindingVnicType: String,
  createdAt: LocalDateTime,
  dataPlaneStatus: String,
  description: String,
  deviceId: String,
  deviceOwner: String,
  dnsAssignment: Map[String, String],
  dnsDomain: String,
  dnsName: String,
  extraDhcpOpts: Seq[Map[String,String]],
  fixedIps: Seq[String],
  ipAllocation: String,
  macAddress: String,
  name: String,
  networkId: String,
  portSecurityEnabled: Boolean,
  projectId: String,
  qosNetworkPolicyId: String,
  qosPolicyId: String,
  revisionNumber: Integer,
  resourceRequest: Option[Map[String, String]],
  securityGrous: Seq[String],
  status: String,
  tags: Seq[String],
  tenantId: String,
  updatedAt: LocalDateTime,
  uplinkStatusPropagation: Boolean,
  macLearningEnabled: Boolean
)
