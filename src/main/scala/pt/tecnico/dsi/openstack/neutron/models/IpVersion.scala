package pt.tecnico.dsi.openstack.neutron.models

import enumeratum.{CirceEnum, Enum, EnumEntry}

// TODO: No longer needed when https://github.com/Comcast/ip4s/pull/177 is released
// the encoder/decoder will still be needed
sealed trait IpVersion extends EnumEntry
case object IpVersion extends Enum[IpVersion] with CirceEnum[IpVersion] { self =>
  case object IPv4 extends IpVersion
  case object IPv6 extends IpVersion
  val values: IndexedSeq[IpVersion] = findValues
}
