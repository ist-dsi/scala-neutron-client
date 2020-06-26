package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import org.http4s.{Header, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.neutron.models.{Subnet, SubnetCreate, SubnetUpdate}

class SubnetsService[F[_]: Sync](baseUri: Uri, authToken: Header)
  (implicit client: Client[F]) extends AsymmetricCrudService[F, Subnet](baseUri, "subnet", authToken)
  with BulkCreate[F, Subnet] {
  override type Update = SubnetUpdate
  override type Create = SubnetCreate
}
