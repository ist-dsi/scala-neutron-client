package pt.tecnico.dsi.openstack.neutron.models

import java.time.OffsetDateTime
import cats.effect.Sync
import com.comcast.ip4s.IpAddress
import io.circe.derivation.{deriveCodec, deriveDecoder, deriveEncoder, renaming}
import io.circe.{Codec, Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient
import pt.tecnico.dsi.openstack.neutron.models.Router.{ConntrackHelper, ExternalGatewayInfo}

object Router {
  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
  case class Create(
    name: String,
    description: Option[String] = None,
    adminStateUp: Boolean = true,
    externalGatewayInfo: Option[ExternalGatewayInfo] = None,
    // its weird not being able to set the routes
    distributed: Option[Boolean] = None,
    ha: Option[Boolean] = None,
    availabilityZoneHints: Option[List[String]] = None,
    projectId: Option[String] = None,
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
    adminStateUp: Option[Boolean] = None,
    externalGatewayInfo: Option[ExternalGatewayInfo] = None,
    routes: Option[List[Route[IpAddress]]] = None,
    distributed: Option[Boolean] = None,
    ha: Option[Boolean] = None,
  ) {
    lazy val needsUpdate: Boolean = {
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, adminStateUp, externalGatewayInfo, distributed, ha, routes).exists(_.isDefined)
    }
  }
  
  /*
  sealed trait Updatable[+T]
  case object KeepExistingValue extends Updatable[Nothing]
  case object Unset extends Updatable[Nothing]
  object ChangeTo {
    def apply[T](valueOption: Option[T]): Updatable[T] = valueOption.fold(Unset: Updatable[T])(new ChangeTo(_))
    def apply[T](value: T): Updatable[T] = new ChangeTo(value)
  }
  case class ChangeTo[T] private (value: T) extends Updatable[T]
  
  implicit def updatableEncoder[T](implicit valueEncoder: Encoder[T]): Encoder[Updatable[T]] = {
    case ChangeTo(value) => valueEncoder(value)
    case Unset => Json.Null
    case KeepExistingValue =>
      // Something that we can then use to filter out from the JsonObject
  }
  
  
  // https://github.com/circe/circe/issues/584
  // Foo(bar = None) = {}                                               // KeepExistingValue
  // Foo(bar = Some(None)) = {"bar": null}                              // Unset
  // Foo(bar = Some(Some("actual data"))) = {"bar": "actual data"}      // Change value
  // We cannot use nulls as the user might invoke dropNullValues or deepDropNullValues, although of she does we can never unset
  // Foo(bar = None) =                {}                // KeepExistingValue
  // Foo(bar = Some(None)) =          {"bar": null}     // Unset
  // Foo(bar = Some(Some("value"))) = {"bar": "value"}  // Change value
  
  case class Update2(
    name: Updatable[String] = KeepExistingValue,
    description: Updatable[String] = KeepExistingValue,
    adminStateUp: Updatable[Boolean] = KeepExistingValue,
    externalGatewayInfo: Updatable[Option[ExternalGatewayInfo]] = KeepExistingValue,
    routes: Updatable[List[Route[IpAddress]]] = KeepExistingValue,
    distributed: Updatable[Boolean] = KeepExistingValue,
    ha: Updatable[Boolean] = KeepExistingValue,
  )
  */
  
  object ConntrackHelper {
    implicit val decoder: Decoder[ConntrackHelper] = deriveDecoder(renaming.snakeCase)
  }
  case class ConntrackHelper(protocol: String, helper: String, port: Int)
  
  object ExternalIp {
    implicit val codec: Codec[ExternalIp] = deriveCodec(renaming.snakeCase)
  }
  case class ExternalIp(subnetId: String, ipAddress: Option[IpAddress] = None) {
    def subnet[F[_]](implicit neutron: NeutronClient[F]): F[Subnet[IpAddress]] = neutron.subnets(subnetId)
    
    def prevalingIp(existing: Option[IpAddress]): Option[IpAddress] = {
      // If ipAddress is setting an address even if its the same as existing, then ipAddress prevails in relationship with the existing one
      // otherwise the existing one wins. The existing will prevail when ipAddress = None (most likely from a Create) and existing = Some.
      // Or in other words, when we created we didn't care about which IP we got but once it got created and an IP address as been assigned
      // we don't want to change it.
      ipAddress.orElse(existing)
    }
  }
  
  object ExternalGatewayInfo {
    implicit val codec: Codec[ExternalGatewayInfo] = deriveCodec(renaming.snakeCase)
  }
  case class ExternalGatewayInfo(networkId: String, enableSnat: Boolean, externalFixedIps: List[ExternalIp])
  
  implicit val decoder: Decoder[Router] = deriveDecoder(Map(
    "revision" -> "revision_number"
  ).withDefault(renaming.snakeCase))
}
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
) extends Identifiable {
  def project[F[_]: Sync](implicit keystone: KeystoneClient[F]): F[Project] = keystone.projects(projectId)
}
