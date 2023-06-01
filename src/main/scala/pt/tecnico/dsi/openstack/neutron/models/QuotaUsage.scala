package pt.tecnico.dsi.openstack.neutron.models

import cats.derived.derived
import cats.derived.ShowPretty
import io.circe.derivation.{ConfiguredCodec, renaming}
import io.circe.{Codec, Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.Usage

object QuotaUsage:
  // Another point for Openstack consistency </sarcasm>
  given [T: Encoder: Decoder]: Codec[Usage[T]] = ConfiguredCodec.derive[Usage[T]](Map("inUse" -> "used").withDefault(renaming.snakeCase))
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
) derives ConfiguredCodec, ShowPretty