package pt.tecnico.dsi.openstack.neutron

import cats.effect.Sync
import org.http4s.Uri
import org.http4s.client.Client
import pt.tecnico.dsi.openstack.keystone.models.{ClientBuilder, Session}
import pt.tecnico.dsi.openstack.neutron.services._

object NeutronClient extends ClientBuilder {
  final type OpenstackClient[F[_]] = NeutronClient[F]
  final val `type`: String = "network"
  
  override def apply[F[_]: Sync: Client](baseUri: Uri, session: Session): NeutronClient[F] =
    new NeutronClient[F](baseUri, session)
}
class NeutronClient[F[_]: Sync](baseUri: Uri, session: Session)(implicit client: Client[F]) {
  val uri: Uri = if (baseUri.path.dropEndsWithSlash.toString.endsWith("v2.0")) baseUri else baseUri / "v2.0"

  val securityGroups: SecurityGroups[F] = new SecurityGroups[F](uri, session)
  val securityGroupRules: SecurityGroupRules[F] = new SecurityGroupRules[F](uri, session)
  val routers: Routers[F] = new Routers[F](uri, session)
  val subnets: Subnets[F] = new Subnets[F](uri, session)
  val quotas: Quotas[F] = new Quotas[F](uri, session)
  
  val networks: Networks[F] = new Networks[F](uri, session)
  val ports: Ports[F] = new Ports[F](uri, session)
  
  /*400 BadRequest {
    "NeutronError": {
      "message": "Invalid input for operation: Requested subnet with cidr: 192.168.199.0/24 for network: fec54963-1ed8-43df-85ef-a1f881f9eb9a overlaps with another subnet.",
      "type": "InvalidInput",
      "detail": ""
    }
  }*/
}
