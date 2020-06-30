package pt.tecnico.dsi.neutron.models

import java.time.LocalDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Port {

  object Read {
    implicit val codec: Decoder[Read] = deriveDecoder(renaming.snakeCase)
  }

  object Create {
    implicit val codec: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }

  object Update {
    implicit val codec: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }

  case class Update(
    name: Option[String] = None,
    adminStateUp: Option[Boolean] = None,
    allowedAddressPairs: Option[List[String]] = None,
    bindingHostId: Option[String] = None,
    bindingProfile: Option[Map[String,String]] = None,
    bindingVnicType: Option[String] = None,
    dataPlaneStatus: Option[String] = None,
    deviceId: Option[String] = None,
    description: Option[String] = None,
    deviceOwner: Option[String] = None,
    dnsName: Option[String] = None,
    dnsDomain: Option[String] = None,
    extraDhcpOpts: Option[List[Map[String,String]]] = None,
    fixedIps: Option[List[String]] = None,
    macAddress: Option[String] = None,
    securityGrous: Option[List[String]] = None,
    qosPolicyId: Option[String] = None,
    macLearningEnabled: Option[Boolean] = None,
  )

  case class Create(
    uplinkStatusPropagation: Option[Boolean] = None,
    macLearningEnabled: Option[Boolean] = None,
    securityGrous: Option[List[String]] = None,
    qosPolicyId: Option[String] = None,
    projectId: Option[String] = None,
    name: Option[String] = None,
    networkId: Option[String] = None,
    portSecurityEnabled: Option[Boolean] = None,
    adminStateUp: Option[Boolean] = None,
    allowedAddressPairs: Option[List[String]] = None,
    bindingHostId: Option[String] = None,
    bindingProfile: Option[Map[String,String]] = None,
    bindingVifType: Option[String] = None,
    deviceId: Option[String] = None,
    deviceOwner: Option[String] = None,
    dnsName: Option[String] = None,
    dnsDomain: Option[String] = None,
    extraDhcpOpts: Option[List[Map[String,String]]] = None,
    fixedIps: Option[List[String]] = None,
    macAddress: Option[String] = None,
  )

  case class Read(
    adminStateUp: Boolean,
    allowedAddressPairs: List[String],
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
    extraDhcpOpts: List[Map[String,String]],
    fixedIps: List[String],
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
    securityGrous: List[String],
    status: String,
    tags: List[String],
    updatedAt: LocalDateTime,
    uplinkStatusPropagation: Boolean,
    macLearningEnabled: Boolean
  )
}

sealed trait Port extends Model {
  type Update = Port.Update
  type Create = Port.Create
  type Read = Port.Read

  implicit val e: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  implicit val g: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  implicit val f: Decoder[Read] = deriveDecoder(renaming.snakeCase)
}

