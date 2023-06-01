package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.derived.derived
import cats.derived.ShowPretty
import com.comcast.ip4s.{Cidr, IpAddress, IpVersion}
import io.circe.derivation.{Configuration, ConfiguredCodec, ConfiguredEncoder, renaming}
import io.circe.syntax.*
import io.circe.{Codec, Encoder}
import org.typelevel.cats.time.instances.offsetdatetime.given
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient

object Subnet:
  private val baseRenames = Map(
    "revision" -> "revision_number",
    "gateway" -> "gateway_ip",
    "nameservers" -> "dns_nameservers",
    "mode" -> "ipv6_address_mode",
    "routerAdvertisementMode" -> "ipv6_ra_mode",
  ).withDefault(renaming.snakeCase)

  given Configuration = Configuration.default.withDefaults.withTransformMemberNames(baseRenames)

  object Create:
    given Encoder[Create[IpAddress]] =
      given Encoder[IpVersion] = ipVersionIntEncoder
      val derived = ConfiguredEncoder.derived[Create[IpAddress]]
      (create: Create[IpAddress]) => {
        val addressOption = create.cidr.map(_.address)
                                  .orElse(create.gateway)
                                  .orElse(create.allocationPools.flatMap(_.headOption).map(_.start))
                                  .orElse(create.hostRoutes.headOption.map(_.nexthop))
        // TODO: if cidr.address != cidr.prefix (eg: 192.168.1.35/27) we could assume cidr.address to be the gateway
        addressOption match
          case None =>
            // The user has not set cidr, gatewayIp, allocationPools, or host routes.
            // Most probably just subnetpoolId/useDefaultSubnetpool, lets hope it did not forget to set IpVersion
            // TODO: should we set it to IPv4?
            derived(create)
          case Some(ip) =>
            // Even if ip_version was already set, we override it to ensure it has the right value.
            derived(create).mapObject(_.add("ip_version", ip.fold(_ => 4, _ => 6).asJson))
      }
    given ShowPretty[Create[IpAddress]] = ShowPretty.derived
  case class Create[+IP <: IpAddress](
    name: String,
    networkId: String,
    description: String = "",
    cidr: Option[Cidr[IP]] = None,
    gateway: Option[IP] = None,
    allocationPools: Option[List[AllocationPool[IP]]] = None,
    hostRoutes: List[Route[IP]] = List.empty,
    nameservers: List[IP] = List.empty,
    enableDhcp: Boolean = true,
    subnetpoolId: Option[String] = None,
    useDefaultSubnetpool: Option[Boolean] = None,
    prefixlen: Option[Int] = None,
    mode: Option[Ipv6Mode] = None,
    routerAdvertisementMode: Option[Ipv6Mode] = None,
    ipVersion: Option[IpVersion] = None,
    segmentId: Option[String] = None,
    serviceTypes: List[String] = List.empty,
    projectId: Option[String] = None
  )
  
  object Update:
    given Encoder[Update[IpAddress]] = ConfiguredEncoder.derived
    given ShowPretty[Update[IpAddress]] = ShowPretty.derived
  case class Update[+IP <: IpAddress](
    name: Option[String] = None,
    description: Option[String] = None,
    gatewayIp: Option[IP] = None,
    allocationPools: Option[List[AllocationPool[IP]]] = None,
    hostRoutes: Option[List[Route[IP]]] = None,
    dnsNameservers: Option[List[IP]] = None,
    enableDhcp: Option[Boolean] = None,
    segmentId: Option[String] = None,
    serviceTypes: Option[List[String]] = None,
  ):
    lazy val needsUpdate: Boolean =
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, gatewayIp, allocationPools, hostRoutes, dnsNameservers, enableDhcp, segmentId, serviceTypes).exists(_.isDefined)

  given Codec[Subnet[IpAddress]] = ConfiguredCodec.derived
  given ShowPretty[Subnet[IpAddress]] = ShowPretty.derived
case class Subnet[+IP <: IpAddress](
  id: String,
  name: String,
  description: String,
  projectId: String,
  
  networkId: String,
  cidr: Cidr[IP],
  gateway: Option[IP] = None,
  allocationPools: List[AllocationPool[IP]],
  hostRoutes: List[Route[IP]],
  enableDhcp: Boolean,
  nameservers: List[IP],
  subnetpoolId: Option[String] = None,
  segmentId: Option[String] = None,
  serviceTypes: List[String],
  // IPv6 specific fields. These are optional even for IPv6 Subnets
  mode: Option[Ipv6Mode] = None,
  routerAdvertisementMode: Option[Ipv6Mode] = None,
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String] = List.empty,
  links: List[Link] = List.empty,
) extends Identifiable {
  def project[F[_]](using keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
  def network[F[_]](using neutron: NeutronClient[F]): F[Network] = neutron.networks(networkId)
}