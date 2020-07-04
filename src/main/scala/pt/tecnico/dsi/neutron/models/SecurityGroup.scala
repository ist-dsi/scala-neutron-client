package pt.tecnico.dsi.neutron.models

import java.time.LocalDateTime

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, renaming, deriveEncoder}

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
    description: Option[String],
    stateful: Option[Boolean]
  )

  case class Update(
    description: Option[String],
    name: Option[String],
    stateful: Option[Boolean]
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
