package pt.tecnico.dsi.openstack.neutron.models

import scala.annotation.nowarn
import cats.derived
import cats.derived.ShowPretty
import io.circe.derivation.{deriveCodec, renaming}
import io.circe.{Codec, Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.Usage

object QuotaUsage {
  implicit val codec: Codec[QuotaUsage] = {
    // Another point for Openstack consistency </sarcasm>
    @nowarn
    implicit def codec[T: Encoder: Decoder]: Codec[Usage[T]] = deriveCodec(Map("inUse" -> "used").withDefault(renaming.snakeCase))
    deriveCodec(renaming.snakeCase)
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