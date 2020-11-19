package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.flatMap._
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{NeutronError, SubnetPool}

final class SubnetPools[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends CrudService[F, SubnetPool, SubnetPool.Create, SubnetPool.Update](baseUri, "subnetpool", session.authToken) {
  
  override def defaultResolveConflict(existing: SubnetPool, create: SubnetPool.Create, keepExistingElements: Boolean,
    extraHeaders: Seq[Header]): F[SubnetPool] = {
    // TODO: handle keep existing elements: https://stackoverflow.com/questions/64754830/encoder-for-update-endpoint-of-a-rest-api
    val updated = SubnetPool.Update(
      description = Option(create.description).filter(_ != existing.description),
      minPrefixlen = create.minPrefixlen.filter(_ != existing.minPrefixlen),
      maxPrefixlen = create.maxPrefixlen.filter(_ != existing.maxPrefixlen),
      defaultPrefixlen = create.defaultPrefixlen.filter(_ != existing.defaultPrefixlen),
      isDefault = create.isDefault.filter(_ != existing.isDefault),
      defaultQuota = if (create.defaultQuota != existing.defaultQuota) create.defaultQuota else None,
      addressScopeId = if (create.addressScopeId != existing.addressScopeId) create.addressScopeId else None,
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Sync[F].pure(existing)
  }
  
  override def createOrUpdate(create: SubnetPool.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (SubnetPool, SubnetPool.Create) => F[SubnetPool] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders))
  : F[SubnetPool] = {
    // If you ask openstack to create two subnet pools with the same name and **prefixes** it won't complain.
    // We want the create to be idempotent, so we decided to make the name,prefixes unique **within** the project
    create.projectId orElse session.scopedProjectId match {
      case None => super.create(create, extraHeaders: _*)
      case Some(projectId) =>
        // We cannot set a limit because of the prefixes
        list("name" -> create.name, "project_id" -> projectId).flatMap { pools =>
          pools.filter(_.prefixes.intersect(create.prefixes).nonEmpty) match {
            case Nil => super.create(create, extraHeaders: _*)
            case List(existing) =>
              getLogger.info(s"createOrUpdate: found unique $name (id: ${existing.id}) with the correct name, prefixes and projectId.")
              resolveConflict(existing, create)
            case _ =>
              val message =
                s"""Cannot create a $name idempotently because more than one exists with:
                   |name: ${create.name}
                   |prefixes: ${create.prefixes} (another subnet pool contains at least one of these CIDRs)
                   |project: ${create.projectId}""".stripMargin
              Sync[F].raiseError(NeutronError(Conflict.reason, message))
          }
        }
    }
  }
}
