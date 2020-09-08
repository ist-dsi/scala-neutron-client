package pt.tecnico.dsi.openstack.neutron

import java.net.InetAddress

import io.circe.{Decoder, Encoder, JsonObject}

package object models {

  implicit val ipv4Decoder: Decoder[InetAddress] = Decoder.decodeString.map(InetAddress.getByName)
  implicit val ipv4Encoder: Encoder[InetAddress] = Encoder.encodeString.contramap(_.getHostAddress)

  def decoderAfterRename[T](renames: Map[String, String], d: Decoder[T]): Decoder[T] = d.prepare {
    _.withFocus {
      _.mapObject { obj =>
        val newMap = obj.toMap.map { case (key, value) =>
          renames.getOrElse(key, key) -> value
        }
        JsonObject.fromMap(newMap)
      }
    }
  }
}
