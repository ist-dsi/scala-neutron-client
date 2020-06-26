package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import io.circe.Codec
import org.http4s.client.Client
import org.http4s.{Header, Uri}

abstract class CrudService[F[_]: Sync: Client, Model: Codec](baseUri: Uri, name: String, authToken: Header)
  extends AsymmetricCrudService[F, Model](baseUri, name, authToken) {
  type Update = Model
  type Create = Model
}