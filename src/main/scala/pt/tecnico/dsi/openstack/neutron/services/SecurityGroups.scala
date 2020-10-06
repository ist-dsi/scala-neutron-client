package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.flatMap._
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{NeutronError, SecurityGroup}

final class SecurityGroups[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends CrudService[F, SecurityGroup, SecurityGroup.Create, SecurityGroup.Update](baseUri, "security_group", session.authToken) {
  
  override def update(id: String, value: SecurityGroup.Update, extraHeaders: Header*): F[SecurityGroup] =
    super.put(wrappedAt = Some(name), value, uri / id, extraHeaders:_*)
  
  override def defaultResolveConflict(existing: SecurityGroup, create: SecurityGroup.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header])
  : F[SecurityGroup] = {
    val updated = SecurityGroup.Update(
      description = if (!create.description.contains(existing.description)) create.description else None,
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Sync[F].pure(existing)
  }
  override def createOrUpdate(create: SecurityGroup.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (SecurityGroup, SecurityGroup.Create) => F[SecurityGroup] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders))
  : F[SecurityGroup] = {
    // We want the create to be idempotent, so we decided to make the name unique **within** the project
    list(Query.fromPairs(
      "name" -> create.name,
      "project_id" -> create.projectId,
      "limit" -> "2", // We only need to 2 to disambiguate (no need to put extra load on the server)
    )).flatMap {
      case List(_, _) =>
        val message =
          s"""Cannot create a SecurityGroup idempotently because more than one exists with:
             |name: ${create.name}
             |project: ${create.projectId}""".stripMargin
        Sync[F].raiseError(NeutronError(Conflict.reason, message))
      case List(existing) => resolveConflict(existing, create)
      case Nil => super.create(create, extraHeaders:_*)
    }
  }
}
