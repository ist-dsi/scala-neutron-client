package pt.tecnico.dsi.neutron.services

import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.neutron.models.Model
import pt.tecnico.dsi.openstack.common.models.WithId

trait BulkCreate[F[_], T <: Model] { service: CrudService[F, T] =>
  def create(values: Seq[Create])(implicit encoder: Encoder[Create], decoder: Decoder[WithId[Model]]): F[Seq[WithId[Model]]] =
    service.post(wrappedAt = Some(pluralName), values, uri)
}

