package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object SecurityGroup {
  object Create {
    implicit val decoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
  case class Create(name: String, projectId: String, description: String = "")
  
  object Update {
    implicit val decoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  case class Update(name: String, description: Option[String] = None)
  
  implicit val decoder: Decoder[SecurityGroup] = withRenames(deriveDecoder[SecurityGroup](renaming.snakeCase))(
    "revision_number" -> "revision",
    "security_group_rules" -> "rules",
  )
}
case class SecurityGroup(
  id: String,
  projectId: String,
  name: String,
  description: String,
  
  rules: List[SecurityGroupRule],
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String],
  links: List[Link] = List.empty
) extends Identifiable
