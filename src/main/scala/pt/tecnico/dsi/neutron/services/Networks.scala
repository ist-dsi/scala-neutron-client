package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.Network

final class Networks[F[_] : Sync : Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Network](baseUri, "network", authToken) with BulkCreate[F, Network]
