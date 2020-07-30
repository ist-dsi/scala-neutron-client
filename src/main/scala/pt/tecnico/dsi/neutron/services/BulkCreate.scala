package pt.tecnico.dsi.neutron.services

import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.neutron.models.Model

trait BulkCreate[F[_], T <: Model] { service: CrudService[F, T] =>
  def create(values: List[T#Create])(implicit encoder: Encoder[T#Create], decoder: Decoder[T#Read]): F[List[T#Read]] =
    service.post(wrappedAt = Some(pluralName), values, uri)
}

