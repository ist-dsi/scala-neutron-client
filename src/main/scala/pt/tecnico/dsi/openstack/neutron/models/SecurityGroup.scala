package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.derived.derived
import cats.derived.ShowPretty
import io.circe.derivation.{Configuration, ConfiguredCodec, ConfiguredEncoder, renaming}
import io.circe.Encoder
import org.typelevel.cats.time.instances.offsetdatetime.given
import org.http4s.Query
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient

object SecurityGroup:
  case class Create(
    name: String,
    projectId: String,
    description: String = "",
  ) derives ConfiguredEncoder, ShowPretty
  
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
  ) derives ConfiguredEncoder, ShowPretty:
    lazy val needsUpdate: Boolean =
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description).exists(_.isDefined)

  val renames = Map("revision" -> "revision_number").withDefault(renaming.snakeCase)
  given Configuration = Configuration.default.withDefaults.withTransformMemberNames(renames)
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
) extends Identifiable derives ConfiguredCodec, ShowPretty:
  def project[F[_]](using keystone: KeystoneClient[F]): F[Project] =
    keystone.projects(projectId)
  
  def rules[F[_]](using neutron: NeutronClient[F]): F[List[SecurityGroupRule]] =
    neutron.securityGroupRules.list(Query.fromPairs("security_group_id" -> id))
  
  def addRule[F[_]](rule: String => SecurityGroupRule.Create)(using neutron: NeutronClient[F]): F[SecurityGroupRule] =
    neutron.securityGroupRules.create(rule(id))
