package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.flatMap._
import com.comcast.ip4s.IpAddress
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{FloatingIp, NeutronError}

final class FloatingIps[F[_] : Sync : Client](baseUri: Uri, session: Session)
  extends CrudService[F, FloatingIp[IpAddress], FloatingIp.Create[IpAddress], FloatingIp.Update[IpAddress]](baseUri, "floatingip", session.authToken) {
  
  override def update(id: String, update: FloatingIp.Update[IpAddress], extraHeaders: Header*): F[FloatingIp[IpAddress]] =
    super.put(wrappedAt = Some(name), update, uri / id, extraHeaders:_*)
  
  override def defaultResolveConflict(existing: FloatingIp[IpAddress], create: FloatingIp.Create[IpAddress],
    keepExistingElements: Boolean, extraHeaders: Seq[Header]): F[FloatingIp[IpAddress]] = {
    //TODO: handle the case where the create has set a portId
    if (existing.portId.isDefined) {
      // A VM is already using the existing Floating IP so its really a conflict and there is nothing we can do.
      // TODO: we should really implement Show for the domain classes (using kittens)
      val message =
        s"""The following floating ip already exists and its in use (has a port associated with it):
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
      // The floating ip already exists but its not used.
      // TODO: log a message saying the floating ip already existed
      val updated = FloatingIp.Update(
        portId = if (existing.portId != create.portId) create.portId else None,
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
        list(Query.fromPairs(
          "floating_network_id" -> create.floatingNetworkId,
          "project_id" -> projectId)
        ).flatMap { floatingIps =>
          floatingIps.filter { floatingIp =>
            floatingIp.dnsName.contains(dnsName) && floatingIp.dnsDomain.contains(dnsDomain)
          } match {
            case List(_, _) =>
              val message =
                s"""Cannot create a Floating Ip idempotently because more than one exists with:
                   |floating network id: ${create.floatingNetworkId}
                   |project: $projectId
                   |dnsName: $dnsName
                   |dnsDomain: $dnsDomain""".stripMargin
              Sync[F].raiseError(NeutronError(Conflict.reason, message))
            case List(existing) => resolveConflict(existing, create)
            case Nil => super.create(create, extraHeaders:_*)
          }
        }
    }
  }
}
