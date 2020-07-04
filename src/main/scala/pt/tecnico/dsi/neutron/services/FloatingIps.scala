package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.{FloatingIp, Subnet}

final class FloatingIps[F[_]: Sync: Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, FloatingIp](baseUri, "floatingip", authToken) {

}
