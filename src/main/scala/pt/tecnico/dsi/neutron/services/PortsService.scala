package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.{Network, NetworkCreate, NetworkUpdate, Port}

class PortsService[F[_]: Sync](baseUri: Uri, authToken: Header)
  (implicit client: Client[F]) extends AsymmetricCrudService[F, Port](baseUri, "port", authToken)
  with BulkCreate[F, Port] {
  override type Update = NetworkUpdate
  override type Create = NetworkCreate
}
