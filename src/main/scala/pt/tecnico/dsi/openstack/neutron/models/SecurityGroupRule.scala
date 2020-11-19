package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import scala.annotation.nowarn
import cats.derived.ShowPretty
import cats.derived
import cats.effect.Sync
import com.comcast.ip4s.{Cidr, IpAddress, IpVersion}
import io.circe.derivation.{deriveEncoder, renaming}
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient

object SecurityGroupRule {
  object Create {
    implicit val encoder: Encoder[Create] = {
      @nowarn // False negative from the compiler. This Encoder is being used in the deriveEncoder which is a macro.
      implicit val remoteEncoder: Encoder[Either[Cidr[IpAddress], String]] = Encoder.encodeEither("remote_ip_prefix", "remote_group_id")
      val derived = deriveEncoder[Create](Map("ipVersion" -> "ethertype").withDefault(renaming.snakeCase)).mapJsonObject { obj =>
        obj.remove("remote").deepMerge(obj("remote").flatMap(_.asObject).get)
      }
      (create: Create) => create.remote match {
        case Some(Left(cidr)) =>
          // If remote is set to a CIDR we set the ipVersion (even if it was already set)
          derived(create.copy(ipVersion = cidr.address.version))
        case _ => derived(create)
      }
    }
    
    implicit val show: ShowPretty[Create] = derived.semiauto.showPretty
    
    /** Creates a rule allowing the TCP ports in `range` to be accessed by the IPs in `cidr`. */
    def apply(range: Range.Inclusive, cidr: Cidr[IpAddress])(securityGroupId: String): Create = Create(
      securityGroupId,
      direction = Direction.Ingress,
      ipVersion = cidr.address.version,
      protocol = Some("tcp"),
      portRangeMin = Some(range.start),
      portRangeMax = Some(range.`end`),
      remote = Some(Left(cidr)),
    )
    /** Creates a rule allowing the TCP `port` to be accessed by the IPs in `cidr`. */
    def apply(port: Int, cidr: Cidr[IpAddress])(securityGroupId: String): Create = apply(port to port, cidr)(securityGroupId)
    
    /** Creates a rule allowing the TCP ports in `range` to be accessed by machines in `remoteSecurityGroupId`. */
    def apply(range: Range.Inclusive, remoteSecurityGroupId: String, ipVersion: IpVersion)(securityGroupId: String): Create = Create(
      securityGroupId,
      direction = Direction.Ingress,
      ipVersion = ipVersion,
      protocol = Some("tcp"),
      portRangeMin = Some(range.start),
      portRangeMax = Some(range.`end`),
      remote = Some(Right(remoteSecurityGroupId)),
    )
    /** Creates a rule allowing the TCP `port` to be accessed by machines in `remoteSecurityGroupId`. */
    def apply(port: Int, remoteSecurityGroupId: String, ipVersion: IpVersion)(securityGroupId: String): Create =
      apply(port to port, remoteSecurityGroupId, ipVersion)(securityGroupId)
  }
  case class Create(
    securityGroupId: String,
    direction: Direction,
    ipVersion: IpVersion,
    protocol: Option[String] = None, // Option[String | Int] would be better
    portRangeMin: Option[Int] = None,
    portRangeMax: Option[Int] = None,
    remote: Option[Either[Cidr[IpAddress], String]] = None, // Option[Cidr[IpAddress] | String] would be better
    description: String = "",
  )
  
  // Custom decoder mainly because of remote
  implicit val decoder: Decoder[SecurityGroupRule] = (cursor: HCursor) => for {
    id <- cursor.get[String]("id")
    projectId <- cursor.get[String]("project_id")
    description <- cursor.getOrElse("description")("")
    securityGroupId <- cursor.get[String]("security_group_id")
    direction <- cursor.get[Direction]("direction")
    protocol <- cursor.get[Option[String]]("protocol")
    ipVersion <- cursor.get[IpVersion]("ethertype")
    min <- cursor.get[Option[Int]]("port_range_min")
    max <- cursor.get[Option[Int]]("port_range_max")
    remoteCidr <- cursor.get[Option[Cidr[IpAddress]]]("remote_ip_prefix")
    remoteSecurityGroupId <- cursor.get[Option[String]]("remote_group_id")
    remote <- (remoteCidr, remoteSecurityGroupId) match {
      case (Some(cidr), None) => Right(Some(Left(cidr)))
      case (None, Some(securityGroupId)) => Right(Some(Right(securityGroupId)))
      case (None, None) => Right(None)
      case (Some(cidr), Some(securityGroupId)) => Left(DecodingFailure(s"Got both a remote_ip_prefix $cidr and a remote_group_id $securityGroupId", cursor.history))
    }
    revision <- cursor.get[Int]("revision_number")
    createdAt <- cursor.get[OffsetDateTime]("created_at")
    updatedAt <- cursor.get[OffsetDateTime]("updated_at")
  } yield SecurityGroupRule(id, projectId, description, securityGroupId, direction, ipVersion, protocol, min, max, remote, revision, createdAt, updatedAt)
  
  implicit val show: ShowPretty[SecurityGroupRule] = {
    import pt.tecnico.dsi.openstack.common.models.showOffsetDateTime
    derived.semiauto.showPretty
  }
}
case class SecurityGroupRule(
  id: String,
  projectId: String,
  description: String,
  
  securityGroupId: String,
  direction: Direction,
  ipVersion: IpVersion,
  protocol: Option[String] = None, // Option[String | Int] would be better
  portRangeMin: Option[Int] = None,
  portRangeMax: Option[Int] = None,
  remote: Option[Either[Cidr[IpAddress], String]], // Option[Cidr[IpAddress] | String] would be better. Does this really need to be an option?
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  links: List[Link] = List.empty, // Its here just so the SecurityGroupRule is identifiable. Another point for Openstack consistency
) extends Identifiable {
  def project[F[_]: Sync](implicit keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
  def securityGroup[F[_]: Sync](implicit neutron: NeutronClient[F]): F[SecurityGroup] = neutron.securityGroups(securityGroupId)
}
