package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.Port

final class Ports[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Port, Port.Create, Port.Update](baseUri, "port", session.authToken)
  with BulkCreate[F, Port, Port.Create] {
  
  override def update(id: String, value: Port.Update, extraHeaders: Header*): F[Port] =
    super.put(wrappedAt = Some(name), value, uri / id, extraHeaders:_*)
}

