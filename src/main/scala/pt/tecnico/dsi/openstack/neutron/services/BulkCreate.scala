package pt.tecnico.dsi.openstack.neutron.services

import org.http4s.Header
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService

trait BulkCreate[F[_], Model <: Identifiable, Create] extends CrudService[F, Model, Create, ?]:
  def create(values: List[Create], extraHeaders: Header.ToRaw*): F[List[Model]] =
    super.post(wrappedAt = Some(pluralName), values, uri, extraHeaders*)

