package pt.tecnico.dsi.openstack.neutron.services

import org.http4s.Header
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService

trait BulkCreate[F[_], Model <: Identifiable, Create] { service: CrudService[F, Model, Create, _] =>
  def create(values: List[Create], extraHeaders: Header*): F[List[Model]] =
    service.post(wrappedAt = Some(pluralName), values, uri, extraHeaders:_*)
}

