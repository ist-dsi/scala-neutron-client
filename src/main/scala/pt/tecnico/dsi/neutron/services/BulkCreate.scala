package pt.tecnico.dsi.neutron.services

import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.neutron.models.Model
import pt.tecnico.dsi.openstack.common.models.WithId

trait BulkCreate[F[_], T <: Model] { service: CrudService[F, T] =>
  def create(values: List[T#Create])(implicit encoder: Encoder[T#Create], decoder: Decoder[WithId[T#Read]]): F[List[WithId[T#Read]]] =
    service.post(wrappedAt = Some(pluralName), values, uri)
}

