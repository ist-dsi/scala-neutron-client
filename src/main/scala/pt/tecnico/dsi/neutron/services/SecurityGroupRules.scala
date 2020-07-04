package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.neutron.models.SecurityGroupRule
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.common.services.Service

final class SecurityGroupRules[F[_] : Sync : Client](baseUri: Uri, authToken: Header) extends Service[F](authToken) {

  val name = "security-group-rule"
  val pluralName = s"${name}s"
  val uri: Uri = baseUri / pluralName

  def list(): Stream[F, WithId[SecurityGroupRule]] = list(Query.empty)

  def list(query: Query): Stream[F, WithId[SecurityGroupRule]] =
    super.list[WithId[SecurityGroupRule]](pluralName, uri, query)

  def create(value: SecurityGroupRule.Create): F[WithId[SecurityGroupRule]] =
    super.post(value, uri, wrappedAt = Some(name))

  def get(id: String): F[WithId[SecurityGroupRule]] = super.get(uri / id, wrappedAt = Some(name))

  def delete(value: WithId[SecurityGroupRule]): F[Unit] = delete(value.id)

  def delete(id: String): F[Unit] = super.delete(uri / id)
}
