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
  
  override def update(id: String, update: FloatingIp.Update, extraHeaders: Header*): F[FloatingIp[IpAddress]] =
    super.put(wrappedAt = Some(name), update, uri / id, extraHeaders:_*)
  
  //TODO: review floating ip create idempotency
  
  override def defaultResolveConflict(existing: FloatingIp[IpAddress], create: FloatingIp.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header]): F[FloatingIp[IpAddress]] = ???
  override def createOrUpdate(create: FloatingIp.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (FloatingIp[IpAddress], FloatingIp.Create) => F[FloatingIp[IpAddress]] = defaultResolveConflict(_, _, keepExistingElements,
      extraHeaders)): F[FloatingIp[IpAddress]] = ???
}
