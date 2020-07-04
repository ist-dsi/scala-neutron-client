package pt.tecnico.dsi.neutron

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.{FloatingIp, Model, Network, Port, SecurityGroup, Subnet}
import pt.tecnico.dsi.neutron.services._

class NeutronClient[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
  val uri: Uri = baseUri / "v2.0"

  val networks = new CrudService[F, Network] (uri, "network", authToken) with BulkCreate[F, Network]
  val subnets  = new CrudService[F, Subnet](uri, "subnet", authToken) with BulkCreate[F, Subnet]
  val ports    = new CrudService[F, Port](uri, "port", authToken) with BulkCreate[F, Port]

  val floatingIps: CrudService[F, FloatingIp] =
    new CrudService[F, FloatingIp](uri, "floatingip", authToken) {}

  val securityGroups: CrudService[F, SecurityGroup] =
    new CrudService[F, SecurityGroup](uri, "security-group", authToken) {}

  val securityGroupRules: SecurityGroupRules[F] = new SecurityGroupRules[F](uri, authToken)

  val routers: Routers[F] = new Routers[F](uri, authToken)
}
