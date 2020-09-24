package pt.tecnico.dsi.openstack.neutron

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.openstack.neutron.services._

class NeutronClient[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
  val uri: Uri = if (baseUri.path.dropEndsWithSlash.toString.endsWith("v2.0")) baseUri else baseUri / "v2.0"

  val securityGroups: SecurityGroups[F] = new SecurityGroups[F](uri, authToken)
  val routers: Routers[F] = new Routers[F](uri, authToken)
  val subnets: Subnets[F] = new Subnets[F](uri, authToken)
  val quotas: Quotas[F] = new Quotas[F](uri, authToken)
  
  val networks: Networks[F] = new Networks[F](uri, authToken)
  val ports: Ports[F] = new Ports[F](uri, authToken)
  
  /*400 BadRequest {
    "NeutronError": {
      "message": "Invalid input for operation: Requested subnet with cidr: 192.168.199.0/24 for network: fec54963-1ed8-43df-85ef-a1f881f9eb9a overlaps with another subnet.",
      "type": "InvalidInput",
      "detail": ""
    }
  }*/
}
