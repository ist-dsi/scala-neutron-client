package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.flatMap._
import com.comcast.ip4s.IpAddress
import org.http4s.Status.Conflict
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.neutron.models.{AllocationPool, Subnet, ipextensions}

final class Subnets[F[_] : Sync : Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Subnet[IpAddress], Subnet.Create[IpAddress], Subnet.Update[IpAddress]](baseUri, "subnet", authToken)
  with BulkCreate[F, Subnet[IpAddress], Subnet.Create[IpAddress]] {
  
  private def updateFromCreate(create: Subnet.Create[IpAddress], existing: Subnet[IpAddress], extraHeaders: Header*): F[Subnet[IpAddress]] = {
    val Subnet.Create(_, description, _, cidr, gatewayIp, allocationPools, hostRoutes, nameservers, enableDhcp,
    subnetpoolId, _, _, _, _, version, segmentId, serviceTypes, _) = create
    
    val updated = Subnet.Update(
      name = None,
      Option.when(description != existing.description)(create.description),
      gatewayIp.filter(_ != existing.gateway),
      allocationPools.filter(_ != existing.allocationPools),
      Option.when(hostRoutes != existing.hostRoutes)(hostRoutes),
      Option.when(nameservers != existing.nameservers)(nameservers),
      Option.when(enableDhcp != existing.enableDhcp)(enableDhcp),
      if (segmentId != existing.segmentId) segmentId else None,
      Option.when(serviceTypes != existing.serviceTypes)(serviceTypes),
    )
    val existingIpVersion = existing.cidr.address.version
    
    def errorOut(message: String): IllegalArgumentException = new IllegalArgumentException(
      s"Existing subnet (id: ${existing.id}, name: ${existing.name}, projectId: ${existing.projectId}, networkId: ${existing.networkId}) $message")
    
    // We cannot do anything with subnetpoolId, useDefaultSubnetpool, prefixlen, ipv6AddressMode, ipv6RaMode
    (for {
      _ <- Either.cond(version.getOrElse(existingIpVersion) == existingIpVersion, (), errorOut(s"is for $existingIpVersion cannot change it to $version."))
      // TODO: we could search for the default subnetpool, and check its ID against existing.subnetpoolId
      _ <- Either.cond(subnetpoolId == existing.subnetpoolId, (), errorOut(s"has subnetpoolId: ${existing.subnetpoolId} cannot change it to $subnetpoolId."))
      // We cannot update the CIDR directly, that's why we are reimplementing part of its logic
      // If subnet pool id is set then the cidr, gateway and the pools would be set from the pool, so trying to set them would mess things up
      // The same will happen if the user has explicitly set the allocation pools.
      result <- cidr.filter(_ != existing.cidr && existing.subnetpoolId.isEmpty && allocationPools.isEmpty)
        .map(AllocationPool.fromCidrAndGateway(_, gatewayIp)) match {
        case None => Right(updated)
        case Some(None) => Left(errorOut(s"cannot change gatewayIp to ${gatewayIp.get} because it either: is outside of ${cidr.get}; or it collides with the " +
          s"network/broadcast address."))
        case Some(Some((gateway, pools))) =>
          Right(updated.copy(
            gatewayIp = Option.when(gateway != existing.gateway)(gateway),
            allocationPools = Option.when(pools != existing.allocationPools)(pools),
          ))
      }
    } yield result) match {
      case Left(error) => Sync[F].raiseError(error)
      case Right(updated) if updated.needsUpdate => update(existing.id, updated, extraHeaders:_*)
      case _ => Sync[F].pure(existing)
    }
  }
  override def create(create: Subnet.Create[IpAddress], extraHeaders: Header*): F[Subnet[IpAddress]] = {
    // We want the create to be idempotent, so we decided to make the name unique **within** a (project, network).
    create.projectId/* orElse session.scopedProjectId*/ match {
      case None => super.create(create, extraHeaders:_*) // With the above orElse this line will most likely always result in an error
      case Some(projectId) =>
        list(Query.fromPairs(
          "name" -> create.name,
          "project_id" -> projectId,
          "network_id" -> create.networkId,
          "limit" -> "2", // We only need to 2 subnets to disambiguate (no need to put extra load on the server)
        )).compile.toList.flatMap {
          case List(_, _) =>
            // There is more than one subnet with name `create.name`. We do not have enough information to disambiguate between them.
            Sync[F].raiseError(UnexpectedStatus(Conflict)) // TODO: improve the error
          case List(existing) => updateFromCreate(create, existing, extraHeaders:_*)
          case Nil => super.create(create, extraHeaders:_*)
        }
    }
  }
  
  override def update(id: String, value: Subnet.Update[IpAddress], extraHeaders: Header*): F[Subnet[IpAddress]] =
    super.put(wrappedAt = Some(name), value, uri / id, extraHeaders:_*)
}
