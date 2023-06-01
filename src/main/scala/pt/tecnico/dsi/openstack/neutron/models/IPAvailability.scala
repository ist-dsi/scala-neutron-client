package pt.tecnico.dsi.openstack.neutron.models

import cats.derived.derived
import cats.derived.ShowPretty
import com.comcast.ip4s.{Cidr, IpAddress, IpVersion}
import io.circe.Codec
import io.circe.derivation.ConfiguredCodec
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.NeutronClient

object SubnetIpAvailability:
  given Codec[IpVersion] = ipVersionIntCodec
case class SubnetIpAvailability(
  cidr: Cidr[IpAddress],
  ipVersion: IpVersion,
  subnetId: String,
  subnetName: String,
  totalIps: BigInt,
  usedIps: BigInt,
) derives ConfiguredCodec, ShowPretty:
  def subnet[F[_]](using neutron: NeutronClient[F]): F[Subnet[IpAddress]] = neutron.subnets(subnetId)

case class IpAvailability(
  networkId: String,
  networkName: String,
  projectId: String,
  totalIps: BigInt,
  usedIps: BigInt,
  subnetIpAvailability: List[SubnetIpAvailability]
) derives ConfiguredCodec, ShowPretty:
  def network[F[_]](using neutron: NeutronClient[F]): F[Network] = neutron.networks(networkId)
  def project[F[_]](using client: KeystoneClient[F]): F[Project] = client.projects(projectId)


