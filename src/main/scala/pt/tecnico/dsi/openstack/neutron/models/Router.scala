package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import com.comcast.ip4s.IpAddress
import io.circe.derivation.{deriveCodec, deriveDecoder, deriveEncoder, renaming}
import io.circe.{Codec, Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.neutron.models.Router.{ConntrackHelper, ExternalGatewayInfo}

object Router {
  object Create {
    implicit val codec: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
  case class Create(
    name: String,
    description: String = "",
    adminStateUp: Boolean = true,
    externalGatewayInfo: Option[ExternalGatewayInfo] = None,
    distributed: Option[Boolean] = None,
    ha: Option[Boolean] = None,
    availabilityZoneHints: Option[List[String]] = None,
    projectId: Option[String] = None,
  )

  object Update {
    implicit val codec: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
    adminStateUp: Option[Boolean] = None,
    externalGatewayInfo: Option[ExternalGatewayInfo] = None,
    distributed: Option[Boolean] = None,
    ha: Option[Boolean] = None,
    routes: Option[List[Route[IpAddress]]] = None,
  ) {
    lazy val needsUpdate: Boolean = {
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, adminStateUp, externalGatewayInfo, distributed, ha, routes).exists(_.isDefined)
    }
  }

  object ConntrackHelper {
    implicit val decoder: Decoder[ConntrackHelper] = deriveDecoder(renaming.snakeCase)
  }
  case class ConntrackHelper(protocol: String, helper: String, port: Int)
  
  object ExternalIp {
    implicit val codec: Codec[ExternalIp] = deriveCodec(renaming.snakeCase)
  }
  // The IpAddress is probably optional when creating the router, but always exists when reading the router
  case class ExternalIp(subnetId: String, ipAddress: IpAddress) {
    //def subnet[F[_]](implicit client: NeutronClient[F]): F[Subnet[IpAddress]] = client.subnets(subnetId)
  }
  
  object ExternalGatewayInfo {
    implicit val codec: Codec[ExternalGatewayInfo] = deriveCodec(renaming.snakeCase)
  }
  case class ExternalGatewayInfo(networkId: String, enableSnat: Boolean, externalFixedIps: List[ExternalIp])
  
  implicit val codec: Decoder[Router] = withRenames(deriveDecoder[Router](renaming.snakeCase))(
    "revision_number" -> "revision"
  )
}
case class Router(
  id: String,
  name: String,
  description: String,
  projectId: String,
  
  adminStateUp: Boolean,
  status: String,
  externalGatewayInfo: Option[ExternalGatewayInfo] = None,
  routes: List[Route[IpAddress]] = List.empty,
  distributed: Boolean,
  ha: Boolean,
  flavorId: Option[String] = None,
  conntrackHelpers: List[ConntrackHelper] = List.empty,
  availabilityZoneHints: List[String] = List.empty,
  availabilityZones: List[String] = List.empty,
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String] = List.empty,
  links: List[Link] = List.empty
) extends Identifiable