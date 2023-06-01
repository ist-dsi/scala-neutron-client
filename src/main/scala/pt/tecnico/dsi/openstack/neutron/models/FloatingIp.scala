package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.derived.derived
import cats.derived.ShowPretty
import cats.effect.Concurrent
import com.comcast.ip4s.IpAddress
import io.circe.derivation.{Configuration, ConfiguredCodec, ConfiguredEncoder, renaming}
import io.circe.{Codec, Encoder}
import org.typelevel.cats.time.instances.offsetdatetime.given
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient
import pt.tecnico.dsi.openstack.neutron.models.FloatingIp.PortForwarding

object FloatingIp:
  object PortForwarding:
    given Codec[PortForwarding[IpAddress]] = ConfiguredCodec.derived
    given ShowPretty[PortForwarding[IpAddress]] = ShowPretty.derived
  case class PortForwarding[+IP <: IpAddress](
    protocol: String,
    internalIpAddress: IP,
    internalPort: Int,
    externalPort: Int,
  )
  
  object Create:
    given Encoder[Create[IpAddress]] = ConfiguredEncoder.derived
    given ShowPretty[Create[IpAddress]] = ShowPretty.derived
  case class Create[+IP <: IpAddress](
    floatingNetworkId: String,
    portId: Option[String] = None,
    fixedIpAddress: Option[IP] = None,
    floatingIpAddress: Option[IP] = None,
    description: String = "",
    subnetId: Option[String] = None,
    dnsName: Option[String] = None,
    dnsDomain: Option[String] = None,
    projectId: Option[String] = None,
  )
  
  object Update:
    given Encoder[Update[IpAddress]] = ConfiguredEncoder.derived
    given ShowPretty[Update[IpAddress]] = ShowPretty.derived
  case class Update[+IP <: IpAddress](
    portId: Option[String] = None,
    fixedIpAddress: Option[IP] = None,
    description: Option[String] = None,
  ):
    lazy val needsUpdate: Boolean =
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(portId, fixedIpAddress, description).exists(_.isDefined)

  val renames = Map("revision" -> "revision_number").withDefault(renaming.snakeCase)
  given Codec[FloatingIp[IpAddress]] = ConfiguredCodec.derive(renames, useDefaults = true)
  given ShowPretty[FloatingIp[IpAddress]] = ShowPretty.derived
case class FloatingIp[+IP <: IpAddress](
  id: String,
  //name: String, // For consistency with other domain classes this one does not have a name </sarcasm> It could be s"$dnsName.${dnsDomain.}"
  description: String,
  projectId: String,
  
  status: String, // Values are ACTIVE, DOWN and ERROR.
  floatingNetworkId: String,
  dnsName: Option[String] = None,
  dnsDomain: Option[String] = None, // Cannot be ip4s Hostname because it ends with '.'
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
) extends Identifiable:
  def project[F[_]](using keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
  def floatingNetwork[F[_]](using neutron: NeutronClient[F]): F[Network] = neutron.networks(floatingNetworkId)
  def router[F[_]: Concurrent](using neutron: NeutronClient[F]): F[Option[Router]] = routerId match
    case None => Concurrent[F].pure(Option.empty)
    case Some(id) => neutron.routers.get(id)
