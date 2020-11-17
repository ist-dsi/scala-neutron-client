package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.{Show, derived}
import cats.derived.ShowPretty
import cats.effect.Sync
import com.comcast.ip4s.IpAddress
import io.circe.derivation.{deriveCodec, deriveDecoder, deriveEncoder, renaming}
import io.circe.{Codec, Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient
import pt.tecnico.dsi.openstack.neutron.models.Router.{ConntrackHelper, ExternalGatewayInfo}

object Router {
  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Create] = derived.semiauto.showPretty
  }
  case class Create(
    name: String,
    description: Option[String] = None,
    adminStateUp: Boolean = true,
    externalGatewayInfo: Option[ExternalGatewayInfo] = None,
    // its weird not being able to set the routes
    distributed: Option[Boolean] = None,
    ha: Option[Boolean] = None,
    availabilityZoneHints: Option[List[String]] = None,
    projectId: Option[String] = None,
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Update] = derived.semiauto.showPretty
  }
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
    adminStateUp: Option[Boolean] = None,
    externalGatewayInfo: Option[ExternalGatewayInfo] = None,
    routes: Option[List[Route[IpAddress]]] = None,
    distributed: Option[Boolean] = None,
    ha: Option[Boolean] = None,
  ) {
    lazy val needsUpdate: Boolean = {
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, adminStateUp, externalGatewayInfo, distributed, ha, routes).exists(_.isDefined)
    }
  }
  
  object ConntrackHelper {
    implicit val decoder: Decoder[ConntrackHelper] = deriveDecoder(renaming.snakeCase)
    implicit val show: ShowPretty[ConntrackHelper] = derived.semiauto.showPretty
  }
  case class ConntrackHelper(protocol: String, port: Int, helper: String)
  
  object ExternalIp {
    implicit val codec: Codec[ExternalIp] = deriveCodec(renaming.snakeCase)
    implicit val show: Show[ExternalIp] = derived.semiauto.show
  }
  case class ExternalIp(subnetId: String, ipAddress: Option[IpAddress] = None) {
    def subnet[F[_]](implicit neutron: NeutronClient[F]): F[Subnet[IpAddress]] = neutron.subnets(subnetId)
    
    def prevalingIp(existing: Option[IpAddress]): Option[IpAddress] = {
      // If ipAddress is setting an address even if its the same as existing, then ipAddress prevails in relationship with the existing one
      // otherwise the existing one wins. The existing will prevail when ipAddress = None (most likely from a Create) and existing = Some.
      // Or in other words, when we created we didn't care about which IP we got but once it got created and an IP address as been assigned
      // we don't want to change it.
      ipAddress.orElse(existing)
    }
  }
  
  object ExternalGatewayInfo {
    implicit val codec: Codec[ExternalGatewayInfo] = deriveCodec(renaming.snakeCase)
    implicit val show: ShowPretty[ExternalGatewayInfo] = derived.semiauto.showPretty
  }
  case class ExternalGatewayInfo(networkId: String, enableSnat: Boolean, externalFixedIps: List[ExternalIp])
  
  implicit val decoder: Decoder[Router] = deriveDecoder(Map(
    "revision" -> "revision_number"
  ).withDefault(renaming.snakeCase))
  implicit val show: ShowPretty[Router] = {
    import pt.tecnico.dsi.openstack.common.models.showOffsetDateTime
    derived.semiauto.showPretty
  }
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
) extends Identifiable {
  def project[F[_]: Sync](implicit keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
}
