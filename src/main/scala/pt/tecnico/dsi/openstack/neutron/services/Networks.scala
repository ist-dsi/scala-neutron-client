package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import cats.syntax.functor._
import io.circe.Decoder
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.neutron.models.Network

final class Networks[F[_] : Sync : Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Network](baseUri, "network", authToken) with BulkCreate[F, Network] {
  
  /** @return an unsorted list of all the segmentation ids currently in use. */
  val listSegmentationIds: F[List[Int]] = {
    implicit val decoderInt: Decoder[Int] = Decoder.decodeInt.at("provider:segmentation_id")
    super.list[Int](pluralName, uri, Query.fromPairs("fields"-> "provider:segmentation_id")).compile.toList
  }
  
  /** @return the first available segmentation id that is within `begin` <= `id` <= `end`. */
  def firstAvailableSegmentationId(begin: Int, end: Int): F[Option[Int]] = listSegmentationIds.map { ids =>
    val filteredAndSortedIds = ids.filter(i => i >= begin && i <= end).sorted
    // First try to find a gap between the existing ids
    filteredAndSortedIds.sliding(2).collectFirst {
      case a :: b :: Nil if b - a > 1 => a + 1
    }.orElse {
      // if the last element is less than `end` we can return the next element
      filteredAndSortedIds.lastOption.filter(_ < end).map(_ + 1)
    }
  }
}
