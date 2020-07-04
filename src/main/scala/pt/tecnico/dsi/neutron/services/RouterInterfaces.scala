package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.RouterInterface
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.common.services.Service

final class RouterInterfaces[F[_] : Sync : Client](base: Uri, authToken: Header) extends Service[F](authToken) {

  def addBySubnet(subnetId: String): F[WithId[RouterInterface]] =
    put(Map("subnet_id" -> subnetId), base / "add_router_interface", wrappedAt = None)

  def addByPort(portId: String): F[WithId[RouterInterface]] =
    put(Map("port_id" -> portId), base / "add_router_interface", wrappedAt = None)

  def remove(value: RouterInterface.Remove): F[WithId[RouterInterface]] =
    put(value, base / "add_router_interface", wrappedAt = None)
}
