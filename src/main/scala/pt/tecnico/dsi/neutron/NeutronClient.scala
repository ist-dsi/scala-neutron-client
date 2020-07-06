package pt.tecnico.dsi.neutron

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.services._

class NeutronClient[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
  val uri: Uri = if (baseUri.path.endsWith("v2.0") || baseUri.path.endsWith("v2.0/")) baseUri else baseUri / "v2.0"

  def crudService[T <: Model](name: String) (implicit e: Encoder[T#Create], u: Encoder[T#Update], r: Decoder[T#Read]) =

  new CrudService[F, T](uri, name, authToken) with BulkCreate[F, T]

  val networks: CrudService[F, Network] = crudService[Network]("network")
  val subnets: CrudService[F, Subnet] = crudService[Subnet]("subnet")
  val ports: CrudService[F, Port] = crudService[Port]("port")
  val quotas: Quotas[F] = new Quotas[F](uri, authToken)
}
