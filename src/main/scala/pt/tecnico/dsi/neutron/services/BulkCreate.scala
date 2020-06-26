package pt.tecnico.dsi.neutron.services

import io.circe.{Decoder, Encoder}
import org.http4s.Method.POST
import pt.tecnico.dsi.neutron.models.WithId

trait BulkCreate[F[_], R] {
  service: AsymmetricCrudService[F, R] =>

  def create(values: Seq[Create])(implicit encoder: Encoder[Create], decoder: Decoder[WithId[R]]): F[Seq[WithId[R]]] =
    service.expect(POST, values, uri, Some(pluralName))

}

