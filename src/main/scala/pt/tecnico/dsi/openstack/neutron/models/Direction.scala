package pt.tecnico.dsi.openstack.neutron.models

import cats.Show
import enumeratum.EnumEntry.Lowercase
import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed trait Direction extends EnumEntry with Lowercase
case object Direction extends Enum[Direction] with CirceEnum[Direction] {
  case object Ingress extends Direction
  case object Egress extends Direction
  
  val values: IndexedSeq[Direction] = findValues
  
  implicit val show: Show[Direction] = Show.fromToString
}