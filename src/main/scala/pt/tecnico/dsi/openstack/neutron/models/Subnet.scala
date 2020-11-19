package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import scala.annotation.nowarn
import cats.derived
import cats.derived.ShowPretty
import cats.effect.Sync
import com.comcast.ip4s.{Cidr, IpAddress, IpVersion, Ipv4Address, Ipv6Address}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link, showOffsetDateTime}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient
import shapeless.Typeable

object Subnet {
  object Create {
    implicit val encoder: Encoder[Create[IpAddress]] = {
      @nowarn // False negative from the compiler. This Encoder is being used in the deriveEncoder which is a macro.
      implicit val ipVersionEncoder: Encoder[IpVersion] = ipVersionIntEncoder
      val derived = deriveEncoder[Create[IpAddress]](baseRenames)
      (create: Create[IpAddress]) => {
        val addressOption = create.cidr.map(_.address)
                                  .orElse(create.gateway)
                                  .orElse(create.allocationPools.flatMap(_.headOption).map(_.start))
                                  .orElse(create.hostRoutes.headOption.map(_.nexthop))
        // TODO: if cidr.address != cidr.prefix (eg: 192.168.1.35/27) we could assume cidr.address to be the gateway
        addressOption match {
          case None =>
            // The user has not set cidr, gatewayIp, allocationPools, or host routes.
            // Most probably just subnetpoolId/useDefaultSubnetpool, lets hope it did not forget to set IpVersion
            // TODO: should we set it to IPv4?
            derived(create)
          case Some(ip) =>
            // Even if ip_version was already set, we override it to ensure it has the right value.
            derived(create).mapObject(_.add("ip_version", ip.fold(_ => 4, _ => 6).asJson))
        }
      }
    }
    implicit def show[IP <: IpAddress: Typeable]: ShowPretty[Create[IP]] = derived.semiauto.showPretty
  }
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
    ipv6AddressMode: Option[Ipv6Mode] = None,
    ipv6RaMode: Option[Ipv6Mode] = None,
    ipVersion: Option[IpVersion] = None,
    segmentId: Option[String] = None,
    serviceTypes: List[String] = List.empty,
    projectId: Option[String] = None
  )
  
  object Update {
    implicit val encoder: Encoder[Update[IpAddress]] = deriveEncoder(renaming.snakeCase)
    implicit def show[IP <: IpAddress: Typeable]: ShowPretty[Update[IP]] = derived.semiauto.showPretty
  }
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
  ) {
    lazy val needsUpdate: Boolean = {
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, gatewayIp, allocationPools, hostRoutes, dnsNameservers, enableDhcp, segmentId, serviceTypes).exists(_.isDefined)
    }
  }
  
  private val baseRenames = Map(
    "revision" -> "revision_number",
    "gateway" -> "gateway_ip",
    "nameservers" -> "dns_nameservers",
  ).withDefault(renaming.snakeCase)
  implicit val decoderV4: Decoder[SubnetIpv4] = deriveDecoder(baseRenames)
  implicit val decoderV6: Decoder[SubnetIpv6] = deriveDecoder(baseRenames ++ Map(
    "mode" -> "ipv6_address_mode",
    "router_advertisement_mode" -> "ipv6_ra_mode",
  ))
  implicit val decoder: Decoder[Subnet[IpAddress]] = (cursor: HCursor) => ipVersionIntDecoder.at("ip_version")(cursor).flatMap {
    case IpVersion.V4 => decoderV4(cursor)
    case IpVersion.V6 => decoderV6(cursor)
  }
  
  implicit def show[IP <: IpAddress]: ShowPretty[Subnet[IP]] = {
    case v4: SubnetIpv4 => SubnetIpv4.show.showLines(v4)
    case v6: SubnetIpv6 => SubnetIpv6.show.showLines(v6)
  }
}
sealed trait Subnet[+IP <: IpAddress] extends Identifiable {
  def name: String
  def description: String
  def projectId: String
  
  def networkId: String
  def cidr: Cidr[IP]
  def gateway: Option[IP]
  def allocationPools: List[AllocationPool[IP]]
  def hostRoutes: List[Route[IP]]
  def enableDhcp: Boolean
  def nameservers: List[IP]
  def subnetpoolId: Option[String]
  def segmentId: Option[String]
  def serviceTypes: List[String]
  
  def revision: Int
  def createdAt: OffsetDateTime
  def updatedAt: OffsetDateTime
  def tags: List[String]
  
  def project[F[_]: Sync](implicit keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
  def network[F[_]: Sync](implicit neutron: NeutronClient[F]): F[Network] = neutron.networks(networkId)
}

object SubnetIpv4 {
  implicit val show: ShowPretty[SubnetIpv4] = derived.semiauto.showPretty
}
case class SubnetIpv4(
  id: String,
  name: String,
  description: String,
  projectId: String,
  
  networkId: String,
  cidr: Cidr[Ipv4Address],
  gateway: Option[Ipv4Address] = None,
  allocationPools: List[AllocationPool[Ipv4Address]],
  hostRoutes: List[Route[Ipv4Address]],
  enableDhcp: Boolean,
  nameservers: List[Ipv4Address],
  subnetpoolId: Option[String] = None,
  segmentId: Option[String] = None,
  serviceTypes: List[String],
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String] = List.empty,
  links: List[Link] = List.empty,
) extends Subnet[Ipv4Address]

object SubnetIpv6 {
  implicit val show: ShowPretty[SubnetIpv6] = derived.semiauto.showPretty
}
case class SubnetIpv6(
  id: String,
  name: String,
  description: String,
  projectId: String,
  
  networkId: String,
  cidr: Cidr[Ipv6Address],
  gateway: Option[Ipv6Address] = None,
  allocationPools: List[AllocationPool[Ipv6Address]],
  hostRoutes: List[Route[Ipv6Address]],
  enableDhcp: Boolean,
  nameservers: List[Ipv6Address],
  subnetpoolId: Option[String] = None,
  segmentId: Option[String] = None,
  serviceTypes: List[String],
  // IPv6 specific fields
  mode: Ipv6Mode,
  routerAdvertisementMode: Ipv6Mode,
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String] = List.empty,
  links: List[Link] = List.empty,
) extends Subnet[Ipv6Address]
