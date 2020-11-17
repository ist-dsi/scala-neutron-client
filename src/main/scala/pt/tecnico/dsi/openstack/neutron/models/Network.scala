package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.{Parallel, derived}
import cats.derived.ShowPretty
import cats.effect.Sync
import com.comcast.ip4s.IpAddress
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder, Json}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.neutron.NeutronClient
import pt.tecnico.dsi.openstack.neutron.models.Network.Segment

object Network {
  object Segment {
    val providerRenames: Map[String, String] = Map(
      "type" -> "provider:network_type",
      "physicalNetwork" -> "provider:physical_network",
      "segmentationId" -> "provider:segmentation_id",
    ).withDefault(renaming.snakeCase)
    implicit val decoder: Decoder[Segment] = deriveDecoder(providerRenames)
    implicit val encoder: Encoder[Segment] = deriveEncoder(providerRenames)
    
    implicit val show: ShowPretty[Segment] = derived.semiauto.showPretty
  }
  case class Segment(`type`: String, physicalNetwork: Option[String] = None, segmentationId: Option[Int] = None)
  
  private val baseRenames: Map[String, String] = Map(
    "routerExternal" -> "router:external",
    "revision" -> "revision_number",
    "subnetIds" -> "subnets",
  ).withDefault(renaming.snakeCase)
  
  private def encoderHandleSegments[T](derived: Encoder.AsObject[T], getSegments: T => Option[List[Segment]]): Encoder.AsObject[T] = (t: T) => {
    if (getSegments(t).map(_.size).getOrElse(0) == 1) {
      // When the number of segments is 1 Openstack makes it special </sarcasm>
      val obj = derived.encodeObject(t)
      val firstSegmentEntries: Option[Iterable[(String, Json)]] = for {
        segmentsJson <- obj("segments")
        segments <- segmentsJson.asArray
        firstSegment <- segments.headOption
        firstSegmentObj <- firstSegment.asObject
      } yield firstSegmentObj.toIterable
      firstSegmentEntries.getOrElse(Iterable.empty).foldLeft(obj.remove("segments")) {
        case (acc, field) => field +: acc
      }
    } else {
      // The derived encoder already does the right thing.
      derived.encodeObject(t)
    }
  }
  
  object Create {
    implicit val encoder: Encoder.AsObject[Create] = encoderHandleSegments(deriveEncoder[Create](baseRenames), _.segments)
    implicit val show: ShowPretty[Create] = derived.semiauto.showPretty
  }
  case class Create(
    name: String,
    description: Option[String] = None,
    mtu: Option[Int] = None,
    dnsDomain: Option[String] = None,
    segments: Option[List[Segment]] = None,
    adminStateUp: Option[Boolean] = None,
    portSecurityEnabled: Option[Boolean] = None,
    routerExternal: Option[Boolean] = None,
    shared: Option[Boolean] = None,
    isDefault: Option[Boolean] = None,
    availabilityZoneHints: Option[List[String]] = None,
    projectId: Option[String] = None,
  )

  object Update {
    implicit val encoder: Encoder[Update] = encoderHandleSegments(deriveEncoder[Update](baseRenames), _.segments)
    implicit val show: ShowPretty[Update] = derived.semiauto.showPretty
  }
  sealed case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
    mtu: Option[Int] = None,
    dnsDomain: Option[String] = None,
    // Most Networking plug-ins (e.g. ML2 Plugin) and drivers do not support updating any provider related attributes.
    // This is here just for the plugins/drivers that allow it.
    segments: Option[List[Segment]] = None,
    adminStateUp: Option[Boolean] = None,
    portSecurityEnabled: Option[Boolean] = None,
    routerExternal: Option[Boolean] = None,
    shared: Option[Boolean] = None,
    isDefault: Option[Boolean] = None,
  ) {
    lazy val needsUpdate: Boolean = {
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, mtu, dnsDomain, segments, adminStateUp, portSecurityEnabled, routerExternal, shared, isDefault).exists(_.isDefined)
    }
  }
  
  implicit val decoder: Decoder[Network] = deriveDecoder[Network](baseRenames).prepare(_.withFocus(_.mapObject{ obj =>
    /* Openstack is inconsistent. When the network has multiple segment mappings the Json looks like this: {
      "network": {
        "segments": [
          {
            "provider:network_type": "vlan",
            "provider:physical_network": "public",
            "provider:segmentation_id": 2
          },
          {
            "provider:network_type": "flat",
            "provider:physical_network": "default",
            "provider:segmentation_id": 0
          }
        ],
        (...)
      }
    }
    However when it has single segment mapping the Json looks like this: {
      "network": {
        "provider:network_type": "vlan",
        "provider:physical_network": "public",
        "provider:segmentation_id": 2
        (...)
      }
    }
    Aka the fields of the Segment are directly inside the network object, instead of being inside an JsonObject inside segments field.
    */
    if (obj("segments").isDefined) {
      obj
    } else {
      val segmentSettings = for {
        field <- Segment.providerRenames.values
        value <- obj(field)
      } yield field -> value
      obj.add("segments", Json.arr(Json.fromFields(segmentSettings)))
    }
  }))
  implicit val show: ShowPretty[Network] = {
    import pt.tecnico.dsi.openstack.common.models.showOffsetDateTime
    derived.semiauto.showPretty
  }
}
sealed case class Network(
  id: String,
  name: String,
  description: String,
  projectId: String,
  
  adminStateUp: Boolean,
  status: String, // ACTIVE, DOWN, BUILD or ERROR.
  mtu: Int,
  dnsDomain: Option[String] = None, // Cannot be ip4s Hostname because it ends with '.'
  subnetIds: List[String],
  ipv4AddressScope: Option[String],
  ipv6AddressScope: Option[String],
  segments: List[Segment] = List.empty,
  portSecurityEnabled: Boolean,
  routerExternal: Boolean = false,
  shared: Boolean,
  isDefault: Boolean = false, // missing also
  availabilityZoneHints: List[String] = List.empty,
  availabilityZones: List[String] = List.empty,
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String] = List.empty,
  links: List[Link] = List.empty
) extends Identifiable {
  def subnets[F[_]: Sync: Parallel](implicit neutron: NeutronClient[F]): F[List[Subnet[IpAddress]]] = {
    import cats.implicits._
    subnetIds.parTraverse(subnetId => neutron.subnets(subnetId))
  }
}
