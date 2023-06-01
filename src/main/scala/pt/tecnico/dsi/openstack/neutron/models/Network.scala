package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.{Monad, Parallel, derived}
import cats.derived.derived
import cats.derived.ShowPretty
import com.comcast.ip4s.IpAddress
import io.circe.derivation.{Configuration, ConfiguredCodec, ConfiguredEncoder, ConfiguredDecoder, renaming}
import io.circe.{Codec, Decoder, Encoder, Json}
import org.typelevel.cats.time.instances.offsetdatetime.given
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.neutron.NeutronClient
import pt.tecnico.dsi.openstack.neutron.models.Network.Segment

object Network:
  object Segment:
    val providerRenames: Map[String, String] = Map(
      "type" -> "provider:network_type",
      "physicalNetwork" -> "provider:physical_network",
      "segmentationId" -> "provider:segmentation_id",
    ).withDefault(renaming.snakeCase)
    given Codec[Segment] = ConfiguredCodec.derive(transformMemberNames = providerRenames)
  case class Segment(
    `type`: String,
    physicalNetwork: Option[String] = None,
    segmentationId: Option[Int] = None,
  ) derives ShowPretty
  
  given Configuration = Configuration.default.withDefaults.withTransformMemberNames(
    Map(
      "routerExternal" -> "router:external",
      "revision" -> "revision_number",
      "subnetIds" -> "subnets",
    ).withDefault(renaming.snakeCase)
  )
  
  private def encoderHandleSegments[T](derived: Encoder.AsObject[T], getSegments: T => Option[List[Segment]]): Encoder.AsObject[T] = (t: T) => {
    if getSegments(t).map(_.size).getOrElse(0) == 1 then
      // When the number of segments is 1 Openstack makes it special </sarcasm>
      val obj = derived.encodeObject(t)
      val firstSegmentEntries: Option[Iterable[(String, Json)]] = for
        segmentsJson <- obj("segments")
        segments <- segmentsJson.asArray
        firstSegment <- segments.headOption
        firstSegmentObj <- firstSegment.asObject
      yield firstSegmentObj.toIterable
      firstSegmentEntries.getOrElse(Iterable.empty).foldLeft(obj.remove("segments")):
        case (acc, field) => field +: acc
    else
      // The derived encoder already does the right thing.
      derived.encodeObject(t)
  }
  
  object Create:
    given Encoder.AsObject[Create] = encoderHandleSegments(ConfiguredEncoder.derived, _.segments)
  case class Create(
    name: String,
    description: String = "",
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
  ) derives ShowPretty

  object Update:
    given Encoder[Update] = encoderHandleSegments(ConfiguredEncoder.derived, _.segments)
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
  ) derives ShowPretty:
    lazy val needsUpdate: Boolean =
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, mtu, dnsDomain, segments, adminStateUp, portSecurityEnabled, routerExternal, shared, isDefault).exists(_.isDefined)
  
  given Decoder[Network] = ConfiguredDecoder.derived[Network].prepare(_.withFocus(_.mapObject{ obj =>
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
    if obj("segments").isDefined then {
      obj
    } else {
      val segmentSettings = for
        field <- Segment.providerRenames.values
        value <- obj(field)
      yield field -> value
      obj.add("segments", Json.arr(Json.fromFields(segmentSettings)))
    }
  }))
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
) extends Identifiable derives ConfiguredEncoder, ShowPretty:
  def subnets[F[_]: Monad: Parallel](using neutron: NeutronClient[F]): F[List[Subnet[IpAddress]]] =
    import cats.implicits.*
    subnetIds.parTraverse(subnetId => neutron.subnets(subnetId))
