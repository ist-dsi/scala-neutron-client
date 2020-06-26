package pt.tecnico.dsi.neutron.models

import io.circe.Codec
import io.circe.derivation.deriveCodec
import org.http4s.Uri
import org.http4s.circe.{decodeUri, encodeUri}

object Link {
  implicit val codec: Codec.AsObject[Link] = deriveCodec
}
case class Link(rel: String, href: Uri)
