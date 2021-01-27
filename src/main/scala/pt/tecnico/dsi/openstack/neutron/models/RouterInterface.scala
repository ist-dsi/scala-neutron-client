package pt.tecnico.dsi.openstack.neutron.models

import cats.derived
import cats.derived.ShowPretty
import com.comcast.ip4s.IpAddress
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient

object RouterInterface {
  implicit val codec: Codec[RouterInterface] = deriveCodec(Map("routerId" -> "id", "projectId" -> "tenant_id").withDefault(renaming.snakeCase))
  implicit val show: ShowPretty[RouterInterface] = derived.semiauto.showPretty
}
case class RouterInterface(
  routerId: String,
  networkId: String,
  projectId: String,
  subnetId: String,
  portId: String,
  tags: List[String] = List.empty
) {
  def router[F[_]](implicit neutron: NeutronClient[F]): F[Router] = neutron.routers(routerId)
  def network[F[_]](implicit neutron: NeutronClient[F]): F[Network] = neutron.networks(networkId)
  def project[F[_]](implicit keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
  def subnet[F[_]](implicit neutron: NeutronClient[F]): F[Subnet[IpAddress]] = neutron.subnets(subnetId)
}