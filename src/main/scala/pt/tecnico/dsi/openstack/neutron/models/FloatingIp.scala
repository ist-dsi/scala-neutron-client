package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.effect.Sync
import com.comcast.ip4s.IpAddress
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient
import pt.tecnico.dsi.openstack.neutron.models.FloatingIp.PortForwarding

object FloatingIp {
  object PortForwarding {
    implicit val decoder: Decoder[PortForwarding[IpAddress]] = deriveDecoder(renaming.snakeCase)
  }
  case class PortForwarding[+IP <: IpAddress](
    protocol: String,
    internalIpAddress: IP,
    internalPort: Int,
    externalPort: Int,
  )

  object Create {
    implicit val decoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
  case class Create(
    floatingNetworkId: String,
    portId: Option[String] = None,
    fixedIpAddress: Option[String] = None,
    floatingIpAddress: Option[String] = None,
    description: Option[String] = None,
    subnetId: Option[String] = None,
    dnsName: Option[String] = None,
    dnsDomain: Option[String] = None,
    projectId: Option[String] = None,
  )

  object Update {
    implicit val decoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  case class Update(
    portId: Option[String] = None,
    fixedIpAddress: Option[IpAddress] = None,
    description: Option[String] = None,
  )
  
  implicit val decoder: Decoder[FloatingIp[IpAddress]] = deriveDecoder(Map(
    "revision" -> "revision_number"
  ).withDefault(renaming.snakeCase))
}
case class FloatingIp[+IP <: IpAddress](
  id: String,
  //name: String, // For consistency with other domain classes this one does not have a name </sarcasm> It could be s"$dnsName.${dnsDomain.}"
  description: Option[String],
  projectId: String,
  
  status: String, // Values are ACTIVE, DOWN and ERROR.
  floatingNetworkId: String,
  dnsName: String,
  dnsDomain: String, // Cannot be ip4s Hostname because it ends with '.'
  fixedIpAddress: Option[IP] = None,
  floatingIpAddress: IP,
  routerId: Option[String] = None,
  portId: Option[String] = None,
  portForwardings: List[PortForwarding[IP]] = List.empty,
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String] = List.empty,
  links: List[Link] = List.empty,
) extends Identifiable {
  def project[F[_]: Sync](implicit keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
  def floatingNetwork[F[_]: Sync](implicit neutron: NeutronClient[F]): F[Network] = neutron.networks(floatingNetworkId)
  def router[F[_]: Sync](implicit neutron: NeutronClient[F]): F[Option[Router]] = routerId match {
    case None => Sync[F].pure(Option.empty)
    case Some(id) => neutron.routers.get(id)
  }
}
