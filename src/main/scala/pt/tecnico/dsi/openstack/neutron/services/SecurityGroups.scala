package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.openstack.neutron.models.SecurityGroup

final class SecurityGroups[F[_] : Sync : Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, SecurityGroup](baseUri, "security-group", authToken)
