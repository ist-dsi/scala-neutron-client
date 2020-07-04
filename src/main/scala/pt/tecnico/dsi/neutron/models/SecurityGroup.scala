package pt.tecnico.dsi.neutron.models

import java.time.LocalDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object SecurityGroup {

  object Read {
    implicit val decoder: Decoder[Read] = deriveDecoder(renaming.snakeCase)
  }

  object Update {
    implicit val decoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }

  object Create {
    implicit val decoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }

  case class Create(
    projectId: String,
    description: Option[String] = None,
    stateful: Option[Boolean] = None
  )

  case class Update(
    description: Option[String] = None,
    name: Option[String] = None,
    stateful: Option[Boolean] = None
  )

  case class Read(
    name: String,
    status: String,
    description: String,
    projectId: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    revisionNumber: Integer,
    tags: List[String],
    securityGroupRoles: List[SecurityGroupRule], // Security Group Roles Object
    stateful: Option[String]
  )

}

sealed trait SecurityGroup extends Model {
  type Update = SecurityGroup.Update
  type Create = SecurityGroup.Create
  type Read = SecurityGroup.Read
}
