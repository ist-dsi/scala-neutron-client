package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.flatMap._
import org.http4s.Status.Conflict
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.SecurityGroup

final class SecurityGroups[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends CrudService[F, SecurityGroup, SecurityGroup.Create, SecurityGroup.Update](baseUri, "security_group", session.authToken) {
  
  override def update(id: String, value: SecurityGroup.Update, extraHeaders: Header*): F[SecurityGroup] =
    super.put(wrappedAt = Some(name), value, uri / id, extraHeaders:_*)
  
  private def updateFromCreate(create: SecurityGroup.Create, existing: SecurityGroup, extraHeaders: Header*): F[SecurityGroup] = {
    val SecurityGroup.Create(_, _, description) = create
    
    val updated = SecurityGroup.Update(
      name = None,
      Option.when(description != existing.description)(description),
    )
    
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Sync[F].pure(existing)
  }
  override def create(create: SecurityGroup.Create, extraHeaders: Header*): F[SecurityGroup] = {
    // We want the create to be idempotent, so we decided to make the name unique **within** the project
    list(Query.fromPairs(
      "name" -> create.name,
      "project_id" -> create.projectId,
      "limit" -> "2", // We only need to 2 to disambiguate (no need to put extra load on the server)
    )).compile.toList.flatMap {
      case List(_, _) =>
        // There is more than one subnet with name `create.name`. We do not have enough information to disambiguate between them.
        Sync[F].raiseError(UnexpectedStatus(Conflict)) // TODO: improve the error
      case List(existing) => updateFromCreate(create, existing, extraHeaders:_*)
      case Nil => super.create(create, extraHeaders:_*)
    }
  }
}
