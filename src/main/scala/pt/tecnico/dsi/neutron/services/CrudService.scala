package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.Model
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.common.services.{CrudService => CommonCrudService}

abstract class CrudService[F[_] : Sync : Client, T <: Model]
(baseUri: Uri, name: String, authToken: Header)
  (implicit e: Encoder[T#Create], f: Encoder[T#Update], g: Decoder[T#Read])
  extends CommonCrudService[F, T#Read, T#Create, T#Update](baseUri, name, authToken) {

  type Create = T#Create
  type Update = T#Update
  type Model = T#Read

  override def update(id: String, value: Update): F[WithId[Model]] = super.put(value, uri / id, wrappedAt = Some(name))
}