package pt.tecnico.dsi.openstack.neutron.models

import scala.annotation.nowarn
import cats.{Show, derived}
import com.comcast.ip4s.{Cidr, IpAddress}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Route {
  // nowarn because of a false negative from the compiler. The Encoder and Decoder is being inside the macro deriveCodec.
  @nowarn implicit def encoder[IP <: IpAddress: Encoder]: Encoder[Route[IP]] = deriveEncoder(renaming.snakeCase)
  // nowarn because of a false negative from the compiler. The Decoder is being used in the deriveDecoder which is a macro.
  @nowarn implicit def decoder[IP <: IpAddress: Decoder](implicit @nowarn d: Decoder[Cidr[IP]]): Decoder[Route[IP]] =
    deriveDecoder(renaming.snakeCase)
  
  implicit def ordering[IP <: IpAddress: Ordering]: Ordering[Route[IP]] = Ordering.by(x => (x.destination, x.nexthop))
  
  implicit def show[IP <: IpAddress]: Show[Route[IP]] = derived.semiauto.show
}
final case class Route[+IP <: IpAddress](destination: Cidr[IP], nexthop: IP)
