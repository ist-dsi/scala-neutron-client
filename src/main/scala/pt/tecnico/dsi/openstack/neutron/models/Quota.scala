package pt.tecnico.dsi.openstack.neutron.models

import cats.derived
import cats.derived.ShowPretty
import io.circe.derivation.{deriveCodec, deriveEncoder, renaming}
import io.circe.{Codec, Encoder}

object Quota {
  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Update] = derived.semiauto.showPretty
    val zero: Update = Update(
      floatingip = Some(0),
      network = Some(0),
      port = Some(0),
      rbacPolicy = Some(0),
      router = Some(0),
      securityGroup = Some(0),
      securityGroupRule = Some(0),
      subnet = Some(0),
      subnetpool = Some(0),
    )
  }
  case class Update(
    floatingip: Option[Int] = None,
    network: Option[Int] = None,
    port: Option[Int] = None,
    rbacPolicy: Option[Int] = None,
    router: Option[Int] = None,
    securityGroup: Option[Int] = None,
    securityGroupRule: Option[Int] = None,
    subnet: Option[Int] = None,
    subnetpool: Option[Int] = None,
  ) {
    lazy val needsUpdate: Boolean = {
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(floatingip, network, port, rbacPolicy, router, securityGroup, securityGroupRule, subnet, subnetpool).exists(_.isDefined)
    }
  }
  
  val zero: Quota = Quota(0, 0, 0, 0, 0, 0, 0, 0, 0)
  
  implicit val codec: Codec[Quota] = deriveCodec(renaming.snakeCase)
  implicit val show: ShowPretty[Quota] = derived.semiauto.showPretty
}
/**
 * A value of -1 means no limit.
 * @param floatingip number of floating IP addresses allowed for each project.
 * @param network number of networks allowed for each project.
 * @param port number of ports allowed for each project.
 * @param rbacPolicy number of role-based access control (RBAC) policies for each project.
 * @param router number of routers allowed for each project.
 * @param securityGroup number of security groups allowed for each project.
 * @param securityGroupRule number of security group rules allowed for each project.
 * @param subnet number of subnets allowed for each project.
 * @param subnetpool number of subnet pools allowed for each project.
 */
case class Quota(
  floatingip: Int,
  network: Int,
  port: Int,
  rbacPolicy: Int,
  router: Int,
  securityGroup: Int,
  securityGroupRule: Int,
  subnet: Int,
  subnetpool: Int,
)
