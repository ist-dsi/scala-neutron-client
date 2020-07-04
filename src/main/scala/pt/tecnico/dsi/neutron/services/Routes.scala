package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import io.circe.{Decoder, Encoder, Json}
import org.http4s.Method.PUT
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.Route
import pt.tecnico.dsi.openstack.common.services.Service

final class Routes[F[_] : Sync : Client](base: Uri, authToken: Header) extends Service[F](authToken) {

  import dsl._

  implicit val d: Decoder[List[Route]] = _.downField("router").get("routes")
  implicit val e: Encoder[List[Route]] = e.mapJson(e => Json.obj("router" -> Json.obj("routes" -> e)))

  def add(routes: List[Route]): F[List[Route]] =
    client.expect(PUT(routes, base / "add_extraroutes"))

  def remove(routes: List[Route]): F[List[Route]] =
    client.expect(PUT(routes, base / "remove_extraroutes"))

}
