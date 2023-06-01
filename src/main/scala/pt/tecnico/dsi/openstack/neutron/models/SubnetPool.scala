package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.derived.derived
import cats.derived.ShowPretty
import com.comcast.ip4s.{Cidr, IpAddress, IpVersion}
import io.circe.derivation.{Configuration, ConfiguredCodec, ConfiguredEncoder, renaming}
import io.circe.{Codec, Encoder}
import org.typelevel.cats.time.instances.offsetdatetime.given
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project

object SubnetPool:
  case class Create(
    name: String,
    description: String = "",
    projectId: Option[String] = None,
    prefixes: List[Cidr[IpAddress]], // All the CIDRs must be IPv4, ou IPv6 they cannot be mixed.
    minPrefixlen: Option[Int] = None,
    maxPrefixlen: Option[Int] = None,
    defaultPrefixlen: Option[Int] = None,
    isDefault: Option[Boolean] = None,
    shared: Option[Boolean] = None,
    defaultQuota: Option[Int] = None,
    addressScopeId: Option[String] = None,
  ) derives ConfiguredEncoder, ShowPretty
  
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
    prefixes: Option[List[Cidr[IpAddress]]] = None, // All the CIDRs must be IPv4, ou IPv6 they cannot be mixed.
    minPrefixlen: Option[Int] = None,
    maxPrefixlen: Option[Int] = None,
    defaultPrefixlen: Option[Int] = None,
    isDefault: Option[Boolean] = None,
    defaultQuota: Option[Int] = None,
    addressScopeId: Option[String] = None,
  ) derives ConfiguredEncoder, ShowPretty:
    lazy val needsUpdate: Boolean =
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, prefixes, minPrefixlen, maxPrefixlen, defaultPrefixlen, isDefault, addressScopeId).exists(_.isDefined)

  given Codec[IpVersion] = ipVersionIntCodec

  val renames = Map("revision" -> "revision_number").withDefault(renaming.snakeCase)
  given Configuration = Configuration.default.withDefaults.withTransformMemberNames(renames)
case class SubnetPool(
  id: String,
  name: String,
  description: String,
  projectId: String,
  
  prefixes: List[Cidr[IpAddress]], // All the CIDRs must be IPv4, ou IPv6 they cannot be mixed.
  ipVersion: IpVersion,
  minPrefixlen: Int,
  maxPrefixlen: Int,
  defaultPrefixlen: Int,
  isDefault: Boolean,
  shared: Boolean,
  defaultQuota: Option[Int] = None,
  addressScopeId: Option[String] = None,

  revision: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  tags: List[String],
  links: List[Link] = List.empty
) extends Identifiable derives ConfiguredCodec, ShowPretty:
  def project[F[_]](using keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
