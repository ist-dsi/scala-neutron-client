package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Concurrent
import cats.syntax.flatMap.*
import cats.syntax.show.*
import com.comcast.ip4s.IpAddress
import org.log4s.getLogger
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{FloatingIp, NeutronError}

final class FloatingIps[F[_] : Concurrent : Client](baseUri: Uri, session: Session)
  extends CrudService[F, FloatingIp[IpAddress], FloatingIp.Create[IpAddress], FloatingIp.Update[IpAddress]](baseUri, "floatingip", session.authToken):
  
  override def defaultResolveConflict(existing: FloatingIp[IpAddress], create: FloatingIp.Create[IpAddress],
    keepExistingElements: Boolean, extraHeaders: Seq[Header.ToRaw]): F[FloatingIp[IpAddress]] =
    if existing.portId.isDefined && create.portId != existing.portId then
      // A VM is already using the existing Floating IP, and its not the intended VM (the portIds are different)
      // so its really a conflict and there is nothing we can do.
      val message = show"""The following $name already exists and its in use (has a port associated with it):
                          |$existing""".stripMargin
      Concurrent[F].raiseError(NeutronError(Conflict.reason, message))
    else
      val updated = FloatingIp.Update(
        fixedIpAddress = if existing.fixedIpAddress != create.fixedIpAddress then create.fixedIpAddress else None,
        description = Option(create.description).filter(_ != existing.description),
      )
      if updated.needsUpdate then update(existing.id, updated, extraHeaders*)
      else Concurrent[F].pure(existing)
  override def createOrUpdate(create: FloatingIp.Create[IpAddress], keepExistingElements: Boolean = true, extraHeaders: Seq[Header.ToRaw] = Seq.empty)
    (resolveConflict: (FloatingIp[IpAddress], FloatingIp.Create[IpAddress]) => F[FloatingIp[IpAddress]] = defaultResolveConflict(_, _, keepExistingElements,
      extraHeaders)): F[FloatingIp[IpAddress]] =
    // A floating ip create is, mostly, not idempotent. We want it to be idempotent, so we decided to make the dnsName, dnsDomain unique **within** a project.
    (create.projectId orElse session.scopedProjectId, create.dnsName, create.dnsDomain) match
      case (None, _, _) | (_, None, _) | (_, _, None) =>
        // If either the projectId, dnsName or dnsDomain are not set there is nothing we can do to implement the create idempotently
        super.create(create, extraHeaders*)
      case (Some(projectId), Some(dnsName), Some(dnsDomain)) =>
        // We cannot search for the dnsName or dnsDomain
        list("floating_network_id" -> create.floatingNetworkId, "project_id" -> projectId).flatMap { floatingIps =>
          // So we filter for them on the client side
          floatingIps.filter { floatingIp =>
            floatingIp.dnsName.contains(dnsName) && floatingIp.dnsDomain.contains(dnsDomain)
          } match
            case Nil => super.create(create, extraHeaders*)
            case List(existing) =>
              getLogger.info(s"createOrUpdate: found unique $name (id: ${existing.id}) with the correct dnsName, dnsDomain, projectId, and portId.")
              resolveConflict(existing, create)
            case _ =>
              val message = s"""Cannot create a $name idempotently because more than one exists with:
                               |floating network id: ${create.floatingNetworkId}
                               |project: $projectId
                               |dnsName: $dnsName
                               |dnsDomain: $dnsDomain""".stripMargin
              Concurrent[F].raiseError(NeutronError(Conflict.reason, message))
        }
