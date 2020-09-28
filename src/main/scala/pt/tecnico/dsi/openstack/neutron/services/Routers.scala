package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.flatMap._
import com.comcast.ip4s.IpAddress
import io.circe.{Decoder, Encoder, Json}
import org.http4s.Status.Conflict
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{Port, Route, Router, RouterInterface, Subnet, cidrDecoder, ipDecoder, ipEncoder}

final class Routers[F[_] : Sync : Client](baseUri: Uri, session: Session)
  extends CrudService[F, Router, Router.Create, Router.Update](baseUri, "router", session.authToken) {
  
  override def update(id: String, value: Router.Update, extraHeaders: Header*): F[Router] =
    super.put(wrappedAt = Some(name), value, uri / id, extraHeaders:_*)
  
  final class Operations(val id: String) { self =>
    private def routesOperation(routes: List[Route[IpAddress]], path: String): F[List[Route[IpAddress]]] = {
      // Double wrapping for double the fun </sarcasm>
      // https://github.com/circe/circe/issues/1536
      implicit val e = Encoder[List[Route[IpAddress]]].mapJson(j => Json.obj("routes" -> j))
      implicit val d = Decoder[List[Route[IpAddress]]].at("routes")
      put(wrappedAt = Some(name), routes, baseUri / id / path)
    }
    def add(routes: List[Route[IpAddress]]): F[List[Route[IpAddress]]] = routesOperation(routes, "add_extraroutes")
    def remove(routes: List[Route[IpAddress]]): F[List[Route[IpAddress]]] = routesOperation(routes, "remove_extraroutes")
    
    private def interfacesOperation(`type`: String, id: String, path: String): F[RouterInterface] =
      put(wrappedAt = None, Map(s"${`type`}_id" -> id), baseUri / self.id / path)
    
    def addInterfaceBySubnet(subnetId: String): F[RouterInterface] =
      interfacesOperation("subnet", subnetId, "add_router_interface")
    def addInterfaceByPort(portId: String): F[RouterInterface] =
      interfacesOperation("port", portId, "add_router_interface")
    
    def removeInterfaceBySubnet(subnetId: String): F[RouterInterface] =
      interfacesOperation("subnet", subnetId, "remove_router_interface")
    def removeInterfaceByPort(portId: String): F[RouterInterface] =
      interfacesOperation("port", portId, "remove_router_interface")
    
    // union types would be awesome here:
    //    def addInterface(subject: Subnet | Port): F[RouterInterface] = addInterfaceBySubnet(subject.id)
    //    def removeInterface(subject: Subnet | Port): F[RouterInterface] = removeInterfaceByPort(subject.id)
    def addInterface(subnet: Subnet[_]): F[RouterInterface] = addInterfaceBySubnet(subnet.id)
    def addInterface(port: Port): F[RouterInterface] =  addInterfaceByPort(port.id)
    def removeInterface(subnet: Subnet[_]): F[RouterInterface] = removeInterfaceBySubnet(subnet.id)
    def removeInterface(port: Port): F[RouterInterface] =  removeInterfaceByPort(port.id)
  }
  
  /** Allows performing operations on the router with `id` */
  def on(id: String): Operations = new Operations(id)
  /** Allows performing operations on `router`. */
  def on(router: Router): Operations = on(router.id)
  
  private def updateFromCreate(create: Router.Create, existing: Router, extraHeaders: Header*): F[Router] = {
    val Router.Create(_, description, adminStateUp, externalGatewayInfo, distributed, ha, _, _) = create
    val updated = Router.Update(
      description = Option.when(description != existing.description)(description),
      adminStateUp = Option.when(adminStateUp != existing.adminStateUp)(adminStateUp),
      // Pretty sure this will bite us
      externalGatewayInfo = if (externalGatewayInfo != existing.externalGatewayInfo) externalGatewayInfo else None,
      distributed = if (!distributed.contains(existing.distributed)) distributed else None,
      ha = if (!ha.contains(existing.ha)) ha else None,
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Sync[F].pure(existing)
  }
  override def create(create: Router.Create, extraHeaders: Header*): F[Router] = {
    // A router create is not idempotent because Openstack always creates a new router and never returns a Conflict, whatever the parameters.
    // We want it to be idempotent, so we decided to make the name unique **within** a project.
    create.projectId orElse session.scopedProjectId match {
      case None => super.create(create, extraHeaders:_*)
      case Some(projectId) =>
        list(Query.fromPairs(
          "name" -> create.name,
          "project_id" -> projectId,
          "limit" -> "2")
        ).compile.toList.flatMap {
          case List(_, _) =>
            // There is more than one router with name `create.name`. We do not have enough information to disambiguate between them.
            Sync[F].raiseError(UnexpectedStatus(Conflict)) // TODO: improve the error
          case List(existing) =>updateFromCreate(create, existing, extraHeaders:_*)
          case Nil => super.create(create, extraHeaders:_*)
        }
    }
  }
}
