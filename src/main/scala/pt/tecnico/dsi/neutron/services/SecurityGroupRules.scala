package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.neutron.models.SecurityGroupRule
import pt.tecnico.dsi.openstack.common.services.Service

final class SecurityGroupRules[F[_] : Sync : Client](baseUri: Uri, authToken: Header) extends Service[F](authToken) {

  val name = "security-group-rule"
  val pluralName = s"${name}s"
  val uri: Uri = baseUri / pluralName

  def list(): Stream[F, SecurityGroupRule] = list(Query.empty)

  def list(query: Query): Stream[F, SecurityGroupRule] = super.list[SecurityGroupRule](pluralName, uri, query)

  def create(value: SecurityGroupRule.Create): F[SecurityGroupRule] =
    super.post(wrappedAt = Some(name), value, uri)

  def get(id: String): F[Option[SecurityGroupRule]] = super.get(wrappedAt = Some(name), uri / id)

  def delete(value: SecurityGroupRule): F[Unit] = delete(value.id)
  def delete(id: String): F[Unit] = super.delete(uri / id)
}
