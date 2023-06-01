package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.derived.ShowPretty
import cats.Show
import cats.derived.derived
import com.comcast.ip4s.IpAddress
import io.circe.derivation.{Configuration, ConfiguredCodec, ConfiguredEncoder, renaming}
import io.circe.Encoder
import org.typelevel.cats.time.instances.offsetdatetime.given
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient
import pt.tecnico.dsi.openstack.neutron.models.Router.{ConntrackHelper, ExternalGatewayInfo}

object Router:
  case class Create(
    name: String,
    description: String = "",
    adminStateUp: Boolean = true,
    externalGatewayInfo: Option[ExternalGatewayInfo] = None,
    // its weird not being able to set the routes
    distributed: Option[Boolean] = None,
    ha: Option[Boolean] = None,
    availabilityZoneHints: Option[List[String]] = None,
    projectId: Option[String] = None,
  ) derives ConfiguredEncoder, ShowPretty
  
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
    adminStateUp: Option[Boolean] = None,
    externalGatewayInfo: Option[ExternalGatewayInfo] = None,
    routes: Option[List[Route[IpAddress]]] = None,
    distributed: Option[Boolean] = None,
    ha: Option[Boolean] = None,
  ) derives ConfiguredEncoder, ShowPretty:
    lazy val needsUpdate: Boolean =
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, adminStateUp, externalGatewayInfo, distributed, ha, routes).exists(_.isDefined)
  
  case class ConntrackHelper(
    protocol: String,
    port: Int,
    helper: String,
  ) derives ConfiguredCodec, ShowPretty
  
  case class ExternalIp(
    subnetId: String,
    ipAddress: Option[IpAddress] = None,
  ) derives ConfiguredCodec, ShowPretty:
    def subnet[F[_]](using neutron: NeutronClient[F]): F[Subnet[IpAddress]] = neutron.subnets(subnetId)
    
    def prevalingIp(existing: Option[IpAddress]): Option[IpAddress] =
      // If ipAddress is setting an address even if its the same as existing, then ipAddress prevails in relationship with the existing one
      // otherwise the existing one wins. The existing will prevail when ipAddress = None (most likely from a Create) and existing = Some.
      // Or in other words, when we created we didn't care about which IP we got but once it got created and an IP address as been assigned
      // we don't want to change it.
      ipAddress.orElse(existing)
  
  case class ExternalGatewayInfo(
    networkId: String,
    enableSnat: Boolean,
    externalFixedIps: List[ExternalIp],
  ) derives ConfiguredCodec, ShowPretty

  val renames = Map("revision" -> "revision_number").withDefault(renaming.snakeCase)
  given Configuration = Configuration.default.withDefaults.withTransformMemberNames(renames)
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
) extends Identifiable derives ConfiguredCodec, ShowPretty:
  def project[F[_]](using keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
