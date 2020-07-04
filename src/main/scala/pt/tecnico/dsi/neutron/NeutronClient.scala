package pt.tecnico.dsi.neutron

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.services._

class NeutronClient[F[_] : Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {

  val uri: Uri = if (baseUri.path.endsWith("v2.0") || baseUri.path.endsWith("v2.0/")) baseUri else baseUri / "v2.0"

  val networks = new Networks[F](uri, authToken)
  val subnets = new Subnets[F](uri, authToken)
  val ports = new Ports[F](uri, authToken)
  val routers = new Routers[F](uri, authToken)
  val floatingIps = new FloatingIps[F](uri, authToken)
  val securityGroups = new SecurityGroups[F](uri, authToken)
  val securityGroupRules = new SecurityGroupRules[F](uri, authToken)
}
