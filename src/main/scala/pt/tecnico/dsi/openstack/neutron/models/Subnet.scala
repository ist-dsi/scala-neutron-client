package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import com.comcast.ip4s.{Cidr, Hostname, IpAddress, Ipv4Address, Ipv6Address}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import io.circe.syntax._
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object Subnet {
  object Create {
    implicit def encoder: Encoder[Create[IpAddress]] = {
      implicit val ipVersionEncoder: Encoder[IpVersion] = Encoder[Int].contramap {
        case IpVersion.IPv4 => 4
        case IpVersion.IPv6 => 6
      }
      val derived = withRenames(deriveEncoder[Create[IpAddress]](renaming.snakeCase))("nameservers" -> "dns_nameservers")
      (create: Create[IpAddress]) => {
        val addressOption = create.cidr.map(_.address)
                                  .orElse(create.gatewayIp)
                                  .orElse(create.allocationPools.flatMap(_.headOption).map(_.start))
                                  .orElse(create.hostRoutes.headOption.map(_.nexthop))
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
  }
  case class Create[+IP <: IpAddress](
    name: String,
    description: String = "",
    networkId: String,
    cidr: Option[Cidr[IP]] = None,
    gatewayIp: Option[IP] = None,
    allocationPools: Option[List[AllocationPool[IP]]] = None,
    hostRoutes: List[Route[IP]] = List.empty,
    nameservers: List[Hostname] = List.empty,
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
  }
  case class Update[+IP <: IpAddress](
    name: Option[String] = None,
    description: Option[String] = None,
    gatewayIp: Option[IP] = None,
    allocationPools: Option[List[AllocationPool[IP]]] = None,
    hostRoutes: Option[List[Route[IP]]] = None,
    dnsNameservers: Option[List[Hostname]] = None,
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
  
  private val baseRenames = List(
    "revision_number" -> "revision",
    "gateway_ip" -> "gateway",
    "dns_nameservers" -> "nameservers",
  )
  implicit val decoderV4: Decoder[SubnetIpv4] = withRenames(deriveDecoder[SubnetIpv4](renaming.snakeCase))(baseRenames:_*)
  implicit val decoderV6: Decoder[SubnetIpv6] = withRenames(deriveDecoder[SubnetIpv6](renaming.snakeCase))(
    (baseRenames ++ List(
      "ipv6_address_mode" -> "mode",
      "ipv6_ra_mode" -> "router_advertisement_mode",
    )):_*
  )
  implicit val decoder: Decoder[Subnet[IpAddress]] = (cursor: HCursor) => cursor.get[Int]("ip_version").flatMap {
    case 4 => decoderV4(cursor)
    case 6 => decoderV6(cursor)
    case v => Left(DecodingFailure(s"Invalid ip_version: $v", cursor.history))
  }
}
sealed trait Subnet[+IP <: IpAddress] extends Identifiable {
  def name: String
  def description: String
  def projectId: String
  
  def networkId: String
  def cidr: Cidr[IP]
  def gateway: IP
  def allocationPools: List[AllocationPool[IP]]
  def hostRoutes: List[Route[IP]]
  def enableDhcp: Boolean
  def nameservers: List[Hostname]
  def subnetpoolId: Option[String]
  def segmentId: Option[String]
  def serviceTypes: List[String]
  
  def revision: Int
  def createdAt: OffsetDateTime
  def updatedAt: OffsetDateTime
  def tags: List[String]
}

case class SubnetIpv4(
  id: String,
  name: String,
  description: String,
  projectId: String,
  
  networkId: String,
  cidr: Cidr[Ipv4Address],
  gateway: Ipv4Address,
  allocationPools: List[AllocationPool[Ipv4Address]],
  hostRoutes: List[Route[Ipv4Address]],
  enableDhcp: Boolean,
  nameservers: List[Hostname],
  subnetpoolId: Option[String] = None,
  segmentId: Option[String] = None,
  serviceTypes: List[String],
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String] = List.empty,
  links: List[Link] = List.empty,
) extends Subnet[Ipv4Address]

case class SubnetIpv6(
  id: String,
  name: String,
  description: String,
  projectId: String,
  
  networkId: String,
  cidr: Cidr[Ipv6Address],
  gateway: Ipv6Address,
  allocationPools: List[AllocationPool[Ipv6Address]],
  hostRoutes: List[Route[Ipv6Address]],
  enableDhcp: Boolean,
  nameservers: List[Hostname],
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