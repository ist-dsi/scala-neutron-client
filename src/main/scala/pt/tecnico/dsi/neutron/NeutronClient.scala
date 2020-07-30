package pt.tecnico.dsi.neutron

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.services._

class NeutronClient[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
  val uri: Uri = if (baseUri.path.dropEndsWithSlash.toString.endsWith("v2.0")) baseUri else baseUri / "v2.0"

  val networks: Networks[F] = new Networks[F](uri, authToken)
  val subnets: Subnets[F] = new Subnets[F](uri, authToken)
  val ports: Ports[F] = new Ports[F](uri, authToken)
  val quotas: Quotas[F] = new Quotas[F](uri, authToken)
}
