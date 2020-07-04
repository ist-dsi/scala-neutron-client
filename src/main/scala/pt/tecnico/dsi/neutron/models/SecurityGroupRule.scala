package pt.tecnico.dsi.neutron.models

import java.time.LocalDateTime

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, renaming, deriveEncoder}

object SecurityGroupRule {
  implicit val decoder: Decoder[SecurityGroupRule] = deriveDecoder(renaming.snakeCase)

  object Create {
    implicit val decoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }

  case class Create(
    projectId: Option[String] = None,
    remoteIpPrefix: Option[String] = None,
    portRangeMin: Option[Integer] = None,
    remoteGroupId: Option[String] = None,
    description: Option[String] = None,
    direction: String,
    protocol: Option[String] = None,
    ethertype: Option[String] = None,
    securityGroupId: String,
    portRangeMax: Option[Integer] = None,
  )
}

case class SecurityGroupRule(
  projectId: String,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime,
  remoteIpPrefix: String,
  portRangeMin: Integer,
  remoteGroupId: String,
  description: String,
  direction: String,
  protocol: String,
  ethertype: String,
  securityGroupId: String,
  portRangeMax: Integer,
  revisionNumber: Integer,
)
