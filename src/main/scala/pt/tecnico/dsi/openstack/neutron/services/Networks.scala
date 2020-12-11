package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.Decoder
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{Network, NeutronError}

final class Networks[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Network, Network.Create, Network.Update](baseUri, "network", session.authToken)
    with BulkCreate[F, Network, Network.Create] {
  
  override def defaultResolveConflict(existing: Network, create: Network.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header]): F[Network] = {
    val updated = Network.Update(
      description = Option(create.description).filter(_ != existing.description),
      mtu = create.mtu.filter(_ != existing.mtu),
      dnsDomain = if (create.dnsDomain != existing.dnsDomain) create.dnsDomain else None,
      // Most Networking plug-ins (e.g. ML2 Plugin) and drivers do not support updating any provider related attributes.
      // The openstack we are testing against doesn't allow it. That is why we are not setting the segments.
      adminStateUp = create.adminStateUp.filter(_ != existing.adminStateUp),
      portSecurityEnabled = create.portSecurityEnabled.filter(_ != existing.portSecurityEnabled),
      routerExternal = create.routerExternal.filter(_ != existing.routerExternal),
      shared = create.shared.filter(_ != existing.shared),
      isDefault = create.isDefault.filter(_ != existing.isDefault),
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Sync[F].pure(existing)
  }
  override def createOrUpdate(create: Network.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (Network, Network.Create) => F[Network] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Network] = {
    // If you ask openstack to create two networks with the same name it won't complain. We want to make the name unique **within** a project.
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
            Sync[F].raiseError(NeutronError(Conflict.reason, message))
        }
    }
  }
  
  /** @return an unsorted list of all the segmentation ids currently in use. This is usually a slow operation. */
  val listSegmentationIds: F[List[Int]] = {
    implicit val decoderInt: Decoder[Int] = Decoder.decodeInt.at("provider:segmentation_id")
    super.list[Int](pluralName, uri.withQueryParam("fields", "provider:segmentation_id"))
  }
  
  /** @return the first available segmentation id that is within `begin` <= `id` <= `end`. This is usually a slow operation. */
  def firstAvailableSegmentationId(begin: Int, end: Int): F[Option[Int]] = listSegmentationIds.map { ids =>
    val filteredAndSortedIds = ids.filter(i => i >= begin && i <= end).sorted
    // First try to find a gap between the existing ids
    filteredAndSortedIds.sliding(2).collectFirst {
      case a :: b :: Nil if b - a > 1 => a + 1
      case a :: Nil => a + 1
    }.filter(_ <= end).orElse {
      // if the last element is less than `end` we can return the next element
      filteredAndSortedIds.lastOption.filter(_ < end).map(_ + 1)
    }
  }
}
