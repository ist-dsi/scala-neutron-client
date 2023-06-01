package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Concurrent
import cats.syntax.flatMap.*
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{NeutronError, SecurityGroup}

final class SecurityGroups[F[_]: Concurrent: Client](baseUri: Uri, session: Session)
  extends CrudService[F, SecurityGroup, SecurityGroup.Create, SecurityGroup.Update](baseUri, "security_group", session.authToken):
  
  override def defaultResolveConflict(existing: SecurityGroup, create: SecurityGroup.Create, keepExistingElements: Boolean,
    extraHeaders: Seq[Header.ToRaw])
  : F[SecurityGroup] =
    val updated = SecurityGroup.Update(
      description = Option(create.description).filter(_ != existing.description),
    )
    if updated.needsUpdate then update(existing.id, updated, extraHeaders*)
    else Concurrent[F].pure(existing)
  override def createOrUpdate(create: SecurityGroup.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header.ToRaw] = Seq.empty)
    (resolveConflict: (SecurityGroup, SecurityGroup.Create) => F[SecurityGroup] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders))
  : F[SecurityGroup] =
    // We want the create to be idempotent, so we decided to make the name unique **within** the project
    list("name" -> create.name, "project_id" -> create.projectId, "limit" -> "2").flatMap:
      case Nil => super.create(create, extraHeaders*)
      case List(existing) =>
        getLogger.info(s"createOrUpdate: found unique $name (id: ${existing.id}) with the correct name and projectId.")
        resolveConflict(existing, create)
      case _ =>
        val message =
          s"""Cannot create a $name idempotently because more than one exists with:
             |name: ${create.name}
             |project: ${create.projectId}""".stripMargin
        Concurrent[F].raiseError(NeutronError(Conflict.reason, message))
