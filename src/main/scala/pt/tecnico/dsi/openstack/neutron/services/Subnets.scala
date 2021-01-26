package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Concurrent
import cats.syntax.flatMap._
import com.comcast.ip4s.IpAddress
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{AllocationPool, NeutronError, Subnet}

final class Subnets[F[_]: Concurrent: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Subnet[IpAddress], Subnet.Create[IpAddress], Subnet.Update[IpAddress]](baseUri, "subnet", session.authToken)
    with BulkCreate[F, Subnet[IpAddress], Subnet.Create[IpAddress]] {
  
  private def computeUpdatedCollection[T: Ordering](existing: List[T], create: List[T], keepExistingElements: Boolean): Option[List[T]] = {
    val newElements = if (keepExistingElements) {
      existing ++ create.filter(route => !existing.contains(route))
    } else {
      create
    }
    Option(newElements.sorted).filter(_ != existing.sorted)
  }
  
  override def defaultResolveConflict(existing: Subnet[IpAddress], create: Subnet.Create[IpAddress], keepExistingElements: Boolean,
    extraHeaders: Seq[Header]): F[Subnet[IpAddress]] = {
    val updated = Subnet.Update(
      description = Option(create.description).filter(_ != existing.description),
      gatewayIp = if (create.gateway != existing.gateway) create.gateway else None,
      allocationPools = create.allocationPools.map(_.sorted).filter(_ != existing.allocationPools.sorted),
      hostRoutes = computeUpdatedCollection(existing.hostRoutes, create.hostRoutes, keepExistingElements),
      dnsNameservers = computeUpdatedCollection(existing.nameservers, create.nameservers, keepExistingElements),
      enableDhcp = Option(create.enableDhcp).filter(_ != existing.enableDhcp),
      segmentId = if (create.segmentId != existing.segmentId) create.segmentId else None,
      serviceTypes = computeUpdatedCollection(existing.serviceTypes, create.serviceTypes, keepExistingElements),
    )
    
    def errorOut(message: String): IllegalArgumentException = new IllegalArgumentException(
      s"Existing subnet (id: ${existing.id}, name: ${existing.name}, projectId: ${existing.projectId}, networkId: ${existing.networkId}) $message")
    
    val Subnet.Create(_, _, _, cidr, gatewayIp, allocationPools, _, _, _, subnetpoolId, _, _, _, _, version, _, _, _) = create
    val existingIpVersion = existing.cidr.address.version
    
    // We cannot do anything with subnetpoolId, useDefaultSubnetpool, prefixlen, ipv6AddressMode, ipv6RaMode
    (for {
      _ <- Either.cond(version.getOrElse(existingIpVersion) == existingIpVersion, (), errorOut(s"is for $existingIpVersion cannot change it to $version."))
      // TODO: we could search for the default subnetpool, and check its ID against existing.subnetpoolId
      _ <- Either.cond(subnetpoolId == existing.subnetpoolId, (), errorOut(s"has subnetpoolId: ${existing.subnetpoolId} cannot change it to $subnetpoolId."))
      // We cannot update the CIDR directly, that's why we are reimplementing part of its logic
      // If subnetpoolId is set then the cidr, gateway, and allocationPools were set from that subnet pool, so trying to set them would mess things up
      // The same will happen if the user has explicitly set the allocation pools.
      result <- cidr.filter(_ != existing.cidr && existing.subnetpoolId.isEmpty && allocationPools.isEmpty)
        .map(AllocationPool.fromCidrAndGateway(_, gatewayIp)) match {
        case None => Right(updated)
        case Some(None) => Left(errorOut(s"cannot change gatewayIp to ${gatewayIp.get} because it either: is outside of ${cidr.get}; or it collides with the " +
          s"network/broadcast address."))
        case Some(Some((gateway, pools))) =>
          Right(updated.copy(
            gatewayIp = if (existing.gateway.contains(gateway)) None else Some(gateway),
            allocationPools = Option.when(pools != existing.allocationPools)(pools),
          ))
      }
    } yield result) match {
      case Left(error) => Concurrent[F].raiseError(error)
      case Right(updated) if updated.needsUpdate => update(existing.id, updated, extraHeaders:_*)
      case _ => Concurrent[F].pure(existing)
    }
  }
  override def createOrUpdate(create: Subnet.Create[IpAddress], keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (Subnet[IpAddress], Subnet.Create[IpAddress]) => F[Subnet[IpAddress]] = defaultResolveConflict(_, _, keepExistingElements,
      extraHeaders)): F[Subnet[IpAddress]] = {
    // We want the create to be idempotent, so we decided to make the name unique **within** a (project, network).
    create.projectId orElse session.scopedProjectId match {
      case None => super.create(create, extraHeaders:_*)
      case Some(projectId) =>
        list("name" -> create.name, "project_id" -> projectId, "network_id" -> create.networkId, "limit" -> "2").flatMap {
          case Nil => super.create(create, extraHeaders:_*)
          case List(existing) =>
            getLogger.info(s"createOrUpdate: found unique $name (id: ${existing.id}) with the correct name, networkId, and projectId.")
            resolveConflict(existing, create)
          case _ =>
            val message =
              s"""Cannot create a $name idempotently because more than one exists with:
                 |name: ${create.name}
                 |projectId: ${create.projectId}
                 |networkId: ${create.networkId}""".stripMargin
            Concurrent[F].raiseError(NeutronError(Conflict.reason, message))
        }
    }
  }
}
