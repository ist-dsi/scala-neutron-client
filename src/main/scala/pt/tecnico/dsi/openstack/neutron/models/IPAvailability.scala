package pt.tecnico.dsi.openstack.neutron.models

import scala.annotation.nowarn
import cats.derived
import cats.derived.ShowPretty
import com.comcast.ip4s.{Cidr, IpAddress, IpVersion}
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient

object SubnetIpAvailability {
  implicit val codec: Codec[SubnetIpAvailability] = {
    @nowarn // False negative from the compiler. This Codec is being used in the deriveCodec which is a macro.
    implicit val ipVersionCodec: Codec[IpVersion] = ipVersionIntCodec
    deriveCodec(renaming.snakeCase)
  }
  implicit val show: ShowPretty[SubnetIpAvailability] = derived.semiauto.showPretty
}
case class SubnetIpAvailability(
  cidr: Cidr[IpAddress],
  ipVersion: IpVersion,
  subnetId: String,
  subnetName: String,
  totalIps: BigInt,
  usedIps: BigInt,
) {
  def subnet[F[_]](implicit neutron: NeutronClient[F]): F[Subnet[IpAddress]] = neutron.subnets(subnetId)
}

object IpAvailability {
  implicit val codec: Codec[IpAvailability] = deriveCodec(renaming.snakeCase)
  implicit val show: ShowPretty[IpAvailability] = derived.semiauto.showPretty
}
case class IpAvailability(
  networkId: String,
  networkName: String,
  projectId: String,
  totalIps: BigInt,
  usedIps: BigInt,
  subnetIpAvailability: List[SubnetIpAvailability]
) {
  def network[F[_]](implicit neutron: NeutronClient[F]): F[Network] = neutron.networks(networkId)
  def project[F[_]](implicit client: KeystoneClient[F]): F[Project] = client.projects(projectId)
}


