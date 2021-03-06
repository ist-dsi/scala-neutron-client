package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.derived
import cats.derived.ShowPretty
import io.circe.derivation.{deriveCodec, deriveEncoder, renaming}
import io.circe.{Codec, Encoder}
import io.chrisdavenport.cats.time.offsetdatetimeInstances
import org.http4s.Query
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient

object SecurityGroup {
  object Create {
    implicit val decoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Create] = derived.semiauto.showPretty
  }
  case class Create(
    name: String,
    projectId: String,
    description: String = "",
  )
  
  object Update {
    implicit val decoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Update] = derived.semiauto.showPretty
  }
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
  ) {
    lazy val needsUpdate: Boolean = {
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description).exists(_.isDefined)
    }
  }
  
  implicit val codec: Codec[SecurityGroup] = deriveCodec(Map("revision" -> "revision_number").withDefault(renaming.snakeCase))
  implicit val show: ShowPretty[SecurityGroup] = derived.semiauto.showPretty
}
case class SecurityGroup(
  id: String,
  projectId: String,
  name: String,
  description: String,
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String],
  links: List[Link] = List.empty
) extends Identifiable {
  def project[F[_]](implicit keystone: KeystoneClient[F]): F[Project] =
    keystone.projects(projectId)
  
  def rules[F[_]](implicit neutron: NeutronClient[F]): F[List[SecurityGroupRule]] =
    neutron.securityGroupRules.list(Query.fromPairs("security_group_id" -> id))
  
  def addRule[F[_]](rule: String => SecurityGroupRule.Create)(implicit neutron: NeutronClient[F]): F[SecurityGroupRule] =
    neutron.securityGroupRules.create(rule(id))
}
