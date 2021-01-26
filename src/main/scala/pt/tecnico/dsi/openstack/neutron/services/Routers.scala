package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Concurrent
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.comcast.ip4s.IpAddress
import io.circe.{Decoder, Encoder, Json}
import org.http4s.Status.{Conflict, Successful, NotFound}
import org.http4s.client.Client
import org.http4s.Method.PUT
import org.http4s.{Header, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.Router.{ExternalGatewayInfo, ExternalIp}
import pt.tecnico.dsi.openstack.neutron.models._

final class Routers[F[_] : Concurrent : Client](baseUri: Uri, session: Session)
  extends CrudService[F, Router, Router.Create, Router.Update](baseUri, "router", session.authToken) {
  
  private def computeUpdatedExternalGatewayInfo(existing: Option[ExternalGatewayInfo], create: Option[ExternalGatewayInfo], keepExistingElements: Boolean): Option[ExternalGatewayInfo] =
    (existing, create) match {
      case (None, _) =>
        // The existing router does not have an externalGatewayInfo. So whatever the create sets prevails
        create
      case (Some(_), None) =>
        // The existing router has an externalGatewayInfo but the create is requesting for it not to have it.
        if (keepExistingElements) existing else {
          // This does not unset the existing ExternalGatewayInfo because the jsonEncoder drops the nulls
          // https://stackoverflow.com/questions/64754830/encoder-for-update-endpoint-of-a-rest-api
          create
        }
      case (Some(existingInfo), Some(createInfo)) if createInfo.networkId != existingInfo.networkId =>
        // The external IPs are set using a subnetId which must be a child of the networkId.
        // If the network is different the create prevails without having to consider keepExistingElements because the previous elements cannot be preserved.
        create
      case (Some(existingInfo), Some(createInfo)) =>
        var newExternalIps = List.empty[ExternalIp]
        for (newExternalIp @ ExternalIp(subnetId, newIp) <- createInfo.externalFixedIps) {
          existingInfo.externalFixedIps.find(_.subnetId == subnetId) match {
            case None =>
              // The existing router does not have an external IP for this subnetId
              newExternalIps +:= newExternalIp
            case Some(existingExternalIp) =>
              // If the create is setting an explicit IP use that. Otherwise use the existing one.
              newExternalIps +:= newIp.map(_ => newExternalIp).getOrElse(existingExternalIp)
          }
        }
        if (keepExistingElements) {
          // Add all existing external IPs for which the subnetId isn't in the newExternalIps
          newExternalIps ++= existingInfo.externalFixedIps.filter(externalIp => !newExternalIps.exists(_.subnetId == externalIp.subnetId))
        }
        // We need to sort the fixed IPs because equals on a list considers order
        val newInfo = createInfo.copy(externalFixedIps = newExternalIps.sortBy(_.subnetId))
        val existingInfoSorted = existingInfo.copy(externalFixedIps = existingInfo.externalFixedIps.sortBy(_.subnetId))
        Option.when(newInfo != existingInfoSorted)(newInfo)
    }
  
  override def defaultResolveConflict(existing: Router, create: Router.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header]): F[Router] = {
    val updated = Router.Update(
      description = Option(create.description).filter(_ != existing.description),
      adminStateUp = Option(create.adminStateUp).filter(_ != existing.adminStateUp),
      externalGatewayInfo = computeUpdatedExternalGatewayInfo(existing.externalGatewayInfo, create.externalGatewayInfo, keepExistingElements),
      // routes cannot be set when creating the Router, so we don't have to update them, because consistency </sarcasm>.
      distributed = if (!create.distributed.contains(existing.distributed)) create.distributed else None,
      ha = if (!create.ha.contains(existing.ha)) create.ha else None,
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Concurrent[F].pure(existing)
  }
  override def createOrUpdate(create: Router.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (Router, Router.Create) => F[Router] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Router] = {
    // A router create is not idempotent because Openstack always creates a new router and never returns a Conflict, whatever the parameters.
    // We want it to be idempotent, so we decided to make the name unique **within** a project.
    create.projectId orElse session.scopedProjectId match {
      case None => super.create(create, extraHeaders:_*)
      case Some(projectId) =>
        list("name" -> create.name, "project_id" -> projectId, "limit" -> "2").flatMap {
          case Nil => super.create(create, extraHeaders:_*)
          case List(existing) =>
            getLogger.info(s"createOrUpdate: found unique $name (id: ${existing.id}) with the correct name and projectId.")
            resolveConflict(existing, create)
          case _ =>
            val message =
              s"""Cannot create a $name idempotently because more than one exists with:
                 |name: ${create.name}
                 |project: ${create.projectId}""".stripMargin
            Concurrent[F].raiseError(NeutronError(Conflict.reason, message))
        }
    }
  }
  
  final class Operations(val id: String) { self =>
    import dsl._
    
    private def routesOperation(routes: List[Route[IpAddress]], path: String): F[List[Route[IpAddress]]] = {
      // Double wrapping for double the fun </sarcasm>
      // https://github.com/circe/circe/issues/1536
      implicit val e = Encoder[List[Route[IpAddress]]].mapJson(j => Json.obj("routes" -> j))
      implicit val d = Decoder[List[Route[IpAddress]]].at("routes")
      put(wrappedAt = Some(name), routes, uri / id / path)
    }
    def add(routes: List[Route[IpAddress]]): F[List[Route[IpAddress]]] = routesOperation(routes, "add_extraroutes")
    def remove(routes: List[Route[IpAddress]]): F[List[Route[IpAddress]]] = routesOperation(routes, "remove_extraroutes")
    
    /**
     * If the router interface has already added a None will be returned, otherwise the RouterInterface.
     * Openstack does not provide a way to obtain an already existing router interface. */
    private def addRouterInterface(`type`: String, id: String): F[Option[RouterInterface]] = {
      import dsl._
      val conflicting = """.*?Router already has a port on subnet ([^ ]+)\.""".r
      client.run(PUT.apply(Map(s"${`type`}_id" -> id), uri / self.id / "add_router_interface", authToken)).use {
        case Successful(response) => response.as[RouterInterface].map(ri => Some(ri))
        case response => response.as[NeutronError].flatMap {
          case NeutronError("BadRequest", conflicting(`id`), _) =>
            // Openstack does not provide a way to obtain an already existing router interface.
            // We almost can compute it:
            // search for the existing port (which is created when this method is successful)
            // routerId = self.id
            // networkId = port.networkId
            // projectId = port.projectId
            // subnetId = this should probably be an Option. What happens when `type` = port?
            // portId = port.id
            // tags = port.tags
            Concurrent[F].pure(Option.empty)
          case error => F.raiseError(error)
        }
      }
    }
    
    private def removeRouterInterface(`type`: String, id: String): F[Unit] = {
      // A delete is done with a PUT! </sarcasm>
      val request = PUT.apply(Map(s"${`type`}_id" -> id), uri / self.id / "remove_router_interface", authToken)
      client.run(request).use {
        case Successful(_) | NotFound(_) => F.unit
        case response => defaultOnError(request, response)
      }
    }
    
    def addInterfaceBySubnet(subnetId: String): F[Option[RouterInterface]] =
      addRouterInterface("subnet", subnetId)
    def addInterfaceByPort(portId: String): F[Option[RouterInterface]] =
      addRouterInterface("port", portId)
    
    def removeInterfaceBySubnet(subnetId: String): F[Unit] =
      removeRouterInterface("subnet", subnetId)
    def removeInterfaceByPort(portId: String): F[Unit] =
      removeRouterInterface("port", portId)
    
    def addInterface(subnet: Subnet[_]): F[Option[RouterInterface]] = addInterfaceBySubnet(subnet.id)
    def removeInterface(subnet: Subnet[_]): F[Unit] = removeInterfaceBySubnet(subnet.id)
  }
  
  /** Allows performing operations on the router with `id` */
  def on(id: String): Operations = new Operations(id)
  /** Allows performing operations on `router`. */
  def on(router: Router): Operations = on(router.id)
}
