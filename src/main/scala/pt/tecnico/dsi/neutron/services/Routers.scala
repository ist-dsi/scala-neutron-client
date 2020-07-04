package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.Router

final class Routers[F[_] : Sync : Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Router](baseUri, "router", authToken) {

  def interface(routerId: String): RouterInterfaces[F] = new RouterInterfaces(baseUri / routerId, authToken)

  def extraRoutes(routerId: String): Routes[F] = new Routes(baseUri / routerId, authToken)
}
