package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.Service
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{NeutronError, SecurityGroupRule}

final class SecurityGroupRules[F[_]: Sync: Client](baseUri: Uri, session: Session) extends Service[F](session.authToken) {
  val name = "security_group_rule"
  val pluralName = s"${name}s"
  val uri: Uri = baseUri / pluralName
  protected val wrappedAt: Option[String] = Some(name)

  def stream(pairs: (String, String)*): Stream[F, SecurityGroupRule] = stream(Query.fromPairs(pairs:_*))
  def stream(query: Query): Stream[F, SecurityGroupRule] = super.stream[SecurityGroupRule](pluralName, uri.copy(query = query))
  
  def list(pairs: (String, String)*): F[List[SecurityGroupRule]] = list(Query.fromPairs(pairs:_*))
  def list(query: Query): F[List[SecurityGroupRule]] = super.list[SecurityGroupRule](pluralName, uri.copy(query = query))
  
  def create(create: SecurityGroupRule.Create, extraHeaders: Header*): F[SecurityGroupRule] =
    super.post(wrappedAt, create, uri, extraHeaders:_*)
  
  /**
   * A sort of idempotent create. If a Conflict is received and the existing rule already has the same settings the existing rule
   * will be returned. Otherwise `F` will have an error.
   *
   * Its impossible to implement an idempotent create because the API does not expose an update endpoint.
   *
   * @param create the create settings.
   * @param extraHeaders extra headers to be used. The `authToken` header is always added.
   */
  def createWithDeduplication(create: SecurityGroupRule.Create, extraHeaders: Header*): F[SecurityGroupRule] = {
    val conflictId = """.*?id is ([a-f0-9\\-]*)\.""".r
    postHandleConflictWithError[SecurityGroupRule.Create, SecurityGroupRule, NeutronError](wrappedAt, create, uri, extraHeaders) {
      case error @ NeutronError("SecurityGroupRuleExists", conflictId(id), _) =>
        apply(id).flatMap { existing =>
          val SecurityGroupRule(_, _, description, _, direction, ipVersion, protocol, portRangeMin, portRangeMax, remote, _, _, _, _) = existing
          if (description.contains(create.description) && create.direction == direction && create.ipVersion == ipVersion && create.protocol == protocol &&
            create.portRangeMin == portRangeMin && create.portRangeMax == portRangeMax && create.remote == remote) {
            getLogger.info(s"createOrUpdate: found unique $name (id: ${existing.id}) with the correct " +
              s"description, direction, ipVersion, protocol, portRangMin, portRangeMax, and remote.")
            F.pure(existing)
          } else {
            // The Security Group Rules API does not have an update. The only thing we could do would be to delete the rule and create it again.
            // But that would surely make the users of this method angry.
            F.raiseError(error)
          }
        }
    }
  }

  def get(id: String): F[Option[SecurityGroupRule]] = super.getOption(wrappedAt, uri / id)
  def apply(id: String): F[SecurityGroupRule] =
    get(id).flatMap {
      case Some(rule) => F.pure(rule)
      case None => F.raiseError(new NoSuchElementException(s"""Could not find $name with id "$id"."""))
    }

  def delete(rule: SecurityGroupRule): F[Unit] = delete(rule.id)
  def delete(id: String): F[Unit] = super.delete(uri / id)
}
