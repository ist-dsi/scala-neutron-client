package pt.tecnico.dsi.neutron

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.{FloatingIp, Model, Network, Port, SecurityGroup, Subnet}
import pt.tecnico.dsi.neutron.services._

class NeutronClient[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
  val uri: Uri = baseUri / "v2.0"

  val networks: CrudService[F, Network] =
    new CrudService(uri, "network", authToken) with BulkCreate[F, Network]

  val subnets: CrudService[F, Subnet] =
    new CrudService(uri, "subnet", authToken) with BulkCreate[F, Subnet]

  val ports: CrudService[F, Port] =
    new CrudService(uri, "port", authToken) with BulkCreate[F, Port]

  val floatingIps: CrudService[F, FloatingIp] =
    new CrudService[F, FloatingIp](uri, "floatingip", authToken) with BulkCreate[F, FloatingIp]

  val securityGroups: CrudService[F, SecurityGroup] =
    new CrudService[F, SecurityGroup](uri, "security-group", authToken) with BulkCreate[F, SecurityGroup]

  val securityGroupRules: CrudService[F, SecurityGroupRules] = new SecurityGroupRules[F](uri, authToken)
  val routers: Routers[F] = new Routers[F](uri, authToken)
}
