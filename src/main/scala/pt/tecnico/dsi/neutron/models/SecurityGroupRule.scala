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
    projectId: Option[String],
    remoteIpPrefix: Option[String],
    portRangeMin: Option[Integer],
    remoteGroupId: Option[String],
    description: Option[String],
    direction: String,
    protocol: Option[String],
    ehtertype: Option[String],
    securityGroupId: String,
    portRangeMax: Option[Integer],
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
  ehtertype: String,
  securityGroupId: String,
  portRangeMax: Integer,
  revisionNumber: Integer,
)

