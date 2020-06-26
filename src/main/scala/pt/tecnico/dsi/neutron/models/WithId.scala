package pt.tecnico.dsi.neutron.models

import io.circe.syntax._
import io.circe.{Codec, Decoder, DecodingFailure, Encoder, HCursor}
import org.http4s.Uri
import org.http4s.circe.decodeUri

object WithId {
  implicit val linksDecoder: Decoder[List[Link]] = { cursor: HCursor =>
    // Openstack has two ways to represent links (because why not):
    // This one is mostly used in Keystone
    //   "links": {
    //     "self": "http://example.com/identity/v3/role_assignments",
    //     "previous": null,
    //     "next": null
    //   }
    // This one is mostly used everywhere else
    //   "links": [
    //     {
    //       "href": "http://127.0.0.1:33951/v3/89afd400-b646-4bbc-b12b-c0a4d63e5bd3/volumes/2b955850-f177-45f7-9f49-ecb2c256d161",
    //       "rel": "self"
    //     }, {
    //       "href": "http://127.0.0.1:33951/89afd400-b646-4bbc-b12b-c0a4d63e5bd3/volumes/2b955850-f177-45f7-9f49-ecb2c256d161",
    //       "rel": "bookmark"
    //     }
    val value = cursor.value
    if (value.isArray) Decoder.decodeList[Link].apply(cursor)
    else if (value.isObject) value.dropNullValues.as[Map[String, Uri]].map(_.map((Link.apply _).tupled).toList)
    else Left(DecodingFailure("Links can only be a object or array.", cursor.history))
  }

  implicit def decoder[T: Decoder]: Decoder[WithId[T]] = (cursor: HCursor) => for {
    id <- cursor.get[String]("id")
    link <- cursor.get[List[Link]]("links")
    model <- cursor.as[T]
  } yield WithId(id, model, link)
  implicit def encoder[T: Encoder]: Encoder[WithId[T]] = (a: WithId[T]) => a.model.asJson.mapObject(_.add("id", a.id.asJson))
  implicit def codec[T: Codec]: Codec[WithId[T]] = Codec.from(decoder, encoder)

  import scala.language.implicitConversions
  implicit def toModel[T](withId: WithId[T]): T = withId.model
}
// All Openstack IDs are strings, 99% are random UUIDs
case class WithId[T](id: String, model: T, links: List[Link] = List.empty) {
  lazy val linksMap: Map[String, Uri] = links.map(l => (l.rel, l.href)).toMap
}