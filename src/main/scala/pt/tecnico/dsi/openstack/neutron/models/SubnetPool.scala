package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import scala.annotation.nowarn
import cats.derived
import cats.derived.ShowPretty
import com.comcast.ip4s.{Cidr, IpAddress, IpVersion}
import io.circe.derivation.{deriveCodec, deriveEncoder, renaming}
import io.circe.{Codec, Encoder}
import io.chrisdavenport.cats.time.offsetdatetimeInstances
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project

object SubnetPool {
  object Create {
    implicit val decoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Create] = derived.semiauto.showPretty
  }
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
  )
  
  object Update {
    implicit val decoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Update] = derived.semiauto.showPretty
  }
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
  ) {
    lazy val needsUpdate: Boolean = {
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, prefixes, minPrefixlen, maxPrefixlen, defaultPrefixlen, isDefault, addressScopeId).exists(_.isDefined)
    }
  }
  
  implicit val codec: Codec[SubnetPool] = {
    @nowarn // False negative from the compiler. This Encoder is being used in the deriveDecoder which is a macro.
    implicit val ipVersionCodec: Codec[IpVersion] = ipVersionIntCodec
    deriveCodec(Map("revision" -> "revision_number").withDefault(renaming.snakeCase))
  }
  implicit val show: ShowPretty[SubnetPool] = derived.semiauto.showPretty
}
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
) extends Identifiable {
  def project[F[_]](implicit keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
}
