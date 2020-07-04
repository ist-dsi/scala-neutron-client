package pt.tecnico.dsi.neutron

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.{Model, Network, Port, Router, Subnet}
import pt.tecnico.dsi.neutron.services._

class NeutronClient[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
  val uri: Uri = baseUri / "v2.0"

  def crudService[T <: Model](name: String) (implicit e: Encoder[T#Create], u: Encoder[T#Update], r: Decoder[T#Read]) =
    new CrudService[F, T](uri, name, authToken) with BulkCreate[F, T]

  val networks: CrudService[F, Network] = crudService("network")
  val subnets: CrudService[F, Subnet] = crudService("subnet")
  val ports: CrudService[F, Port] = crudService("port")

  val routers: Routers[F] = new Routers[F](uri, authToken)
}
