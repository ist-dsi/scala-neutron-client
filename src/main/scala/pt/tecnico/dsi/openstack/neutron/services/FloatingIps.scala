package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.flatMap._
import com.comcast.ip4s.IpAddress
import org.log4s.getLogger
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{FloatingIp, NeutronError}

final class FloatingIps[F[_] : Sync : Client](baseUri: Uri, session: Session)
  extends CrudService[F, FloatingIp[IpAddress], FloatingIp.Create[IpAddress], FloatingIp.Update[IpAddress]](baseUri, "floatingip", session.authToken) {
  
  override def defaultResolveConflict(existing: FloatingIp[IpAddress], create: FloatingIp.Create[IpAddress],
    keepExistingElements: Boolean, extraHeaders: Seq[Header]): F[FloatingIp[IpAddress]] = {
    if (existing.portId.isDefined && create.portId != existing.portId) {
      // A VM is already using the existing Floating IP, and its not the intended VM (the portIds are different)
      // so its really a conflict and there is nothing we can do.
      // TODO: should we implement Show for the domain classes (using kittens)
      val message =
        s"""The following $name already exists and its in use (has a port associated with it):
           |id: ${existing.id}
           |description: ${existing.description}
           |project: ${existing.projectId}
           |status: ${existing.status}
           |floatingNetworkId: ${existing.floatingNetworkId}
           |dnsName: ${existing.dnsName}
           |dnsDomain: ${existing.dnsDomain}
           |fixedIpAddress: ${existing.fixedIpAddress}
           |floatingIpAddress: ${existing.floatingIpAddress}
           |routerId: ${existing.routerId}
           |portId: ${existing.portId}
           |portForwardings: ${existing.portForwardings}
           |revision: ${existing.revision}
           |createdAt: ${existing.createdAt}
           |updatedAt: ${existing.updatedAt}""".stripMargin
      Sync[F].raiseError(NeutronError(Conflict.reason, message))
    } else {
      val updated = FloatingIp.Update(
        fixedIpAddress = if (existing.fixedIpAddress != create.fixedIpAddress) create.fixedIpAddress else None,
        description = if (existing.description != create.description) create.description else None,
      )
      if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
      else Sync[F].pure(existing)
    }
  }
  override def createOrUpdate(create: FloatingIp.Create[IpAddress], keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (FloatingIp[IpAddress], FloatingIp.Create[IpAddress]) => F[FloatingIp[IpAddress]] = defaultResolveConflict(_, _, keepExistingElements,
      extraHeaders)): F[FloatingIp[IpAddress]] = {
    // A floating ip create is, mostly, not idempotent. We want it to be idempotent, so we decided to make the dnsName, dnsDomain unique **within** a project.
    (create.projectId orElse session.scopedProjectId, create.dnsName, create.dnsDomain) match {
      case (None, _, _) | (_, None, _) | (_, _, None) =>
        // If either the projectId, dnsName or dnsDomain are not set there is nothing we can do to implement the create idempotently
        super.create(create, extraHeaders:_*)
      case (Some(projectId), Some(dnsName), Some(dnsDomain)) =>
        // We cannot search for the dnsName or dnsDomain
        list("floating_network_id" -> create.floatingNetworkId, "project_id" -> projectId).flatMap { floatingIps =>
          // So we filter for them on the client side
          floatingIps.filter { floatingIp =>
            floatingIp.dnsName.contains(dnsName) && floatingIp.dnsDomain.contains(dnsDomain)
          } match {
            case List(_, _) =>
              val message =
                s"""Cannot create a $name idempotently because more than one exists with:
                   |floating network id: ${create.floatingNetworkId}
                   |project: $projectId
                   |dnsName: $dnsName
                   |dnsDomain: $dnsDomain""".stripMargin
              Sync[F].raiseError(NeutronError(Conflict.reason, message))
            case List(existing) =>
              getLogger.info(s"createOrUpdate: found unique $name (id: ${existing.id}) with the correct dnsName, dnsDomain, projectId, and portId.")
              resolveConflict(existing, create)
            case Nil => super.create(create, extraHeaders:_*)
          }
        }
    }
  }
}
