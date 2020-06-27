package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.neutron.models.{Network, NetworkCreate, NetworkUpdate}

class NetworksService[F[_]: Sync](baseUri: Uri, authToken: Header)
  (implicit client: Client[F]) extends AsymmetricCrudService[F, Network](baseUri, "network", authToken)
  with BulkCreate[F, Network] {
  override type Update = NetworkUpdate
  override type Create = NetworkCreate
}
