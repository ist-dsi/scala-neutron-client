package pt.tecnico.dsi.openstack.neutron.models

import scala.annotation.nowarn
import cats.effect.Sync
import com.comcast.ip4s.{Cidr, IpAddress, IpVersion}
import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient

object SubnetIpAvailability {
  implicit val decoder: Decoder[SubnetIpAvailability] = {
    @nowarn // False negative from the compiler. This Encoder is being used in the deriveDecoder which is a macro.
    implicit val ipVersionDecoder: Decoder[IpVersion] = ipVersionIntDecoder
    deriveDecoder(renaming.snakeCase)
  }
}
case class SubnetIpAvailability(
  cidr: Cidr[IpAddress],
  ipVersion: IpVersion,
  subnetId: String,
  subnetName: String,
  totalIps: BigInt,
  usedIps: BigInt,
) {
  def subnet[F[_]: Sync](implicit neutron: NeutronClient[F]): F[Subnet[IpAddress]] = neutron.subnets(subnetId)
}

object IpAvailability {
  implicit val decoder: Decoder[IpAvailability] = deriveDecoder(renaming.snakeCase)
}
case class IpAvailability(
  networkId: String,
  networkName: String,
  projectId: String,
  totalIps: BigInt,
  usedIps: BigInt,
  subnetIpAvailability: List[SubnetIpAvailability]
) {
  def network[F[_]: Sync](implicit neutron: NeutronClient[F]): F[Network] = neutron.networks(networkId)
  def project[F[_]: Sync](implicit client: KeystoneClient[F]): F[Project] = client.projects(projectId)
}


