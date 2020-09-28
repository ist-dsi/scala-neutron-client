package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import com.comcast.ip4s.{Cidr, IpAddress}
import enumeratum.{Circe, Enum, EnumEntry}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.neutron.models.IpVersion.IPv6
import pt.tecnico.dsi.openstack.neutron.models.SecurityGroupRule.Direction

object SecurityGroupRule {
  sealed trait Direction extends EnumEntry
  case object Direction extends Enum[Direction] {
    implicit val circeEncoder: Encoder[Direction] = Circe.encoderLowercase(this)
    implicit val circeDecoder: Decoder[Direction] = Circe.decoderLowercaseOnly(this)
    
    case object Ingress extends Direction
    case object Egress extends Direction
    
    val values: IndexedSeq[Direction] = findValues
  }
  
  object Create {
    implicit val decoder: Encoder[Create] = {
      implicit val remoteEncoder: Encoder[Either[Cidr[IpAddress], String]] = Encoder.encodeEither("remote_ip_prefix", "remote_group_id")
      val derived = withRenames(deriveEncoder[Create](renaming.snakeCase))("ip_version" -> "ethertype").mapJsonObject { obj =>
        obj.remove("remote").deepMerge(obj("remote").flatMap(_.asObject).get)
      }
      (create: Create) => create.remote match {
        case Some(Left(cidr)) => derived(create.copy(ipVersion = cidr.address.version))
        case _ => derived(create)
      }
    }
    
    def apply(range: Range.Inclusive, cidr: Cidr[IpAddress])(securityGroupId: String): Create = Create(
      securityGroupId,
      direction = Direction.Ingress,
      ipVersion = cidr.address.version,
      protocol = Some("tcp"),
      portRangeMin = Some(range.start),
      portRangeMax = Some(range.`end`),
      remote = Some(Left(cidr)),
    )
    def apply(port: Int, cidr: Cidr[IpAddress])(securityGroupId: String): Create = apply(port to port, cidr)(securityGroupId)
    
    def apply(range: Range.Inclusive, remoteSecurityGroupId: String, ipVersion: IpVersion)(securityGroupId: String): Create = Create(
      securityGroupId,
      direction = Direction.Ingress,
      ipVersion = ipVersion,
      protocol = Some("tcp"),
      portRangeMin = Some(range.start),
      portRangeMax = Some(range.`end`),
      remote = Some(Right(remoteSecurityGroupId)),
    )
    def apply(port: Int, remoteSecurityGroupId: String, ipVersion: IpVersion)(securityGroupId: String): Create =
      apply(port to port, remoteSecurityGroupId, ipVersion)(securityGroupId)
  }
  case class Create(
    securityGroupId: String,
    description: String = "",
    direction: Direction,
    ipVersion: IpVersion,
    protocol: Option[String] = None, // Option[String | Int] would be better
    portRangeMin: Option[Int] = None,
    portRangeMax: Option[Int] = None,
    remote: Option[Either[Cidr[IpAddress], String]] = None, // Option[Cidr[IpAddress] | String] would be better
  )
  
  implicit val decoder: Decoder[SecurityGroupRule] = {
    implicit val remoteDecoder: Decoder[Either[Cidr[IpAddress], String]] = Decoder.decodeEither("remote_ip_prefix", "remote_group_id")
    withRenames(deriveDecoder[SecurityGroupRule](renaming.snakeCase))("revision_number" -> "revision", "ethertype" -> "ip_version")
  }
  /*
  implicit val decoder: Decoder[SecurityGroupRule] = (cursor: HCursor) => for {
    id <- cursor.get[String]("id")
    description <- cursor.get[String]("description")
    projectId <- cursor.get[String]("project_id")
    securityGroupId <- cursor.get[String]("security_group_id")
    direction <- cursor.get[Direction]("direction")
    protocol <- cursor.get[Option[Byte]]("protocol")
    ethertype <- cursor.get[IpVersion]("ethertype")
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
  } yield SecurityGroupRule(id, description, projectId, securityGroupId, direction, protocol, ethertype, min, max, remote, revision, createdAt, updatedAt)
  */
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
  remote: Option[Either[Cidr[IpAddress], String]], // Option[Cidr[IpAddress] | String] would be better
  
  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  links: List[Link] = List.empty, // Its here just so the SecurityGroupRule is identifiable. Another point for Openstack consistency
) extends Identifiable
