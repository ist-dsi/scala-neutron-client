package pt.tecnico.dsi.openstack.neutron.models

import cats.derived.derived
import cats.derived.ShowPretty
import com.comcast.ip4s.IpAddress
import io.circe.derivation.{Configuration, ConfiguredCodec, renaming}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient

object RouterInterface:
  val renames = Map("routerId" -> "id", "projectId" -> "tenant_id").withDefault(renaming.snakeCase)
  given Configuration = Configuration.default.withDefaults.withTransformMemberNames(renames)
case class RouterInterface(
  routerId: String,
  networkId: String,
  projectId: String,
  subnetId: String,
  portId: String,
  tags: List[String] = List.empty
) derives ConfiguredCodec, ShowPretty {
  def router[F[_]](using neutron: NeutronClient[F]): F[Router] = neutron.routers(routerId)
  def network[F[_]](using neutron: NeutronClient[F]): F[Network] = neutron.networks(networkId)
  def project[F[_]](using keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
  def subnet[F[_]](using neutron: NeutronClient[F]): F[Subnet[IpAddress]] = neutron.subnets(subnetId)
}