package pt.tecnico.dsi.openstack.neutron.models

import java.time.LocalDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object SecurityGroup {


  object Update {
    implicit val decoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }

  case class Update(
    description: Option[String] = None,
    name: Option[String] = None,
    stateful: Option[Boolean] = None
  )

  object Create {
    implicit val decoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }

  case class Create(
    projectId: String,
    description: Option[String] = None,
    stateful: Option[Boolean] = None
  )

  object Read {
    implicit val decoder: Decoder[Read] = deriveDecoder(renaming.snakeCase)
  }

  case class Read(
    id: String,
    name: String,
    status: String,
    description: String,
    projectId: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    revisionNumber: Int,
    tags: List[String],
    securityGroupRoles: List[SecurityGroupRule], // Security Group Roles Object
    stateful: Option[String],
    links: List[Link] = List.empty
  ) extends Identifiable
}

sealed trait SecurityGroup extends Model {
  type Update = SecurityGroup.Update
  type Create = SecurityGroup.Create
  type Read = SecurityGroup.Read
}
