package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.Port

final class Ports[F[_] : Sync : Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Port](baseUri, "port", authToken) with BulkCreate[F, Port]

