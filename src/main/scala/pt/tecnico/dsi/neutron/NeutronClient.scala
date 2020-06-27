package pt.tecnico.dsi.neutron

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.services._

class NeutronClient[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
  val uri: Uri = baseUri / "v2.0"
  val networks: NetworksService[F] = new NetworksService[F](uri, authToken)
  val subnets: SubnetsService[F] = new SubnetsService[F](uri, authToken)
}
