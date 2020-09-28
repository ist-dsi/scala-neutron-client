package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import io.circe.Decoder
import org.http4s.Method.POST
import org.http4s.Status.{Conflict, Successful}
import org.http4s.client.Client
import org.http4s.{EntityDecoder, EntityEncoder, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.Service
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{NeutronError, SecurityGroupRule}

final class SecurityGroupRules[F[_]: Sync: Client](baseUri: Uri, session: Session) extends Service[F](session.authToken) {
  import dsl._
  
  val name = "security_group_rule"
  val pluralName = s"${name}s"
  val uri: Uri = baseUri / pluralName

  def list(): Stream[F, SecurityGroupRule] = list(Query.empty)
  def list(query: Query): Stream[F, SecurityGroupRule] = super.list[SecurityGroupRule](pluralName, uri, query)

  def create(create: SecurityGroupRule.Create): F[SecurityGroupRule] = {
    val wrappedAt = Some(name)
    implicit val e: EntityEncoder[F, SecurityGroupRule.Create] = wrapped(wrappedAt)
    implicit val d: EntityDecoder[F, SecurityGroupRule] = unwrapped(wrappedAt)
    POST(create, uri, authToken).flatMap(client.run(_).use {
      case Successful(response) => response.as[SecurityGroupRule]
      case response =>
        val conflictId = """.*?id is ([a-f0-9\\-]*)\.""".r
        response.as[NeutronError].flatMap {
          case error @ NeutronError("SecurityGroupRuleExists", conflictId(id), _) if response.status == Conflict =>
            apply(id).flatMap { existing =>
              val SecurityGroupRule(_, _, description, _, direction, ipVersion, protocol, portRangeMin, portRangeMax, remote, _, _, _, _) = existing
              if (description.contains(create.description) && create.direction == direction && create.ipVersion == ipVersion && create.protocol == protocol &&
                create.portRangeMin == portRangeMin && create.portRangeMax == portRangeMax && create.remote == remote) {
                F.pure(existing)
              } else {
                F.raiseError(error)
              }
            }
          case error => F.raiseError(error)
        }
    })
  }

  def apply(id: String): F[SecurityGroupRule] = super.get(wrappedAt = Some(name), uri / id)
  def get(id: String): F[Option[SecurityGroupRule]] = super.getOption(wrappedAt = Some(name), uri / id)

  def delete(rule: SecurityGroupRule): F[Unit] = delete(rule.id)
  def delete(id: String): F[Unit] = super.delete(uri / id)
}
