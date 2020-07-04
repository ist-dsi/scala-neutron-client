package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.{Network, Subnet}

final class Subnets[F[_]: Sync: Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Subnet](baseUri, "subnet", authToken) with BulkCreate[F, Subnet]
