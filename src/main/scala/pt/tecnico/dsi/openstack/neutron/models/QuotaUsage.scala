package pt.tecnico.dsi.openstack.neutron.models

import scala.annotation.nowarn
import cats.derived
import cats.derived.ShowPretty
import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}
import pt.tecnico.dsi.openstack.common.models.Usage

object QuotaUsage {
  implicit val decoder: Decoder[QuotaUsage] = {
    // Another point for Openstack consistency </sarcasm>
    @nowarn
    implicit def decoder[T: Decoder]: Decoder[Usage[T]] = deriveDecoder(Map("inUse" -> "used").withDefault(renaming.snakeCase))
    deriveDecoder(renaming.snakeCase)
  }
  implicit val show: ShowPretty[QuotaUsage] = derived.semiauto.showPretty
}
case class QuotaUsage(
  floatingip: Usage[Int],
  network: Usage[Int],
  port: Usage[Int],
  rbacPolicy: Usage[Int],
  router: Usage[Int],
  securityGroup: Usage[Int],
  securityGroupRule: Usage[Int],
  subnet: Usage[Int],
  subnetpool: Usage[Int],
)