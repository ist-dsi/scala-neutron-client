package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.Router

class Routers[F[_]: Sync: Client](baseUri: Uri, authToken: Header)
  (implicit e: Encoder[Router#Create], f: Encoder[Router#Update], g: Decoder[Router#Read])
  extends CrudService[F, Router](baseUri, "router", authToken) {

  def interface(routerId: String): RouterInterfaces[F] = new RouterInterfaces(baseUri / routerId, authToken)
  def extraRoutes(routerId: String): Routes[F] = new Routes(baseUri / routerId, authToken)
}
