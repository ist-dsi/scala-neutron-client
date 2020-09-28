package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import com.comcast.ip4s.IpAddress
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.FloatingIp

final class FloatingIps[F[_] : Sync : Client](baseUri: Uri, session: Session)
  extends CrudService[F, FloatingIp[IpAddress], FloatingIp.Create, FloatingIp.Update](baseUri, "floatingip", session.authToken) {
  
  override def update(id: String, value: FloatingIp.Update, extraHeaders: Header*): F[FloatingIp[IpAddress]] =
    super.put(wrappedAt = Some(name), value, uri / id, extraHeaders:_*)
}
