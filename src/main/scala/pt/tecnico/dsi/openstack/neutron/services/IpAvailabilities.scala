package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Concurrent
import fs2.Stream
import io.circe.Decoder
import org.http4s.{Header, Query, Uri}
import org.http4s.client.Client
import pt.tecnico.dsi.openstack.common.services.{PartialCrudService, ListOperations}
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.IpAvailability

final class IpAvailabilities[F[_]: Concurrent: Client](baseUri: Uri, session: Session)
  extends PartialCrudService[F](baseUri, "network_ip_availability", session.authToken)
    with ListOperations[F, IpAvailability] {
  
  override implicit val modelDecoder: Decoder[IpAvailability] = IpAvailability.codec
  
  // This is causing an UninitializedFieldError. That is why it is commented and the methods stream and list are being overridden
  //override val pluralName: String = "network_ip_availabilities"
  override val uri: Uri = baseUri / "network-ip-availabilities" // Because consistency is key </sarcasm>
  
  override def stream(query: Query, extraHeaders: Header*): Stream[F, IpAvailability] =
    stream[IpAvailability]("network_ip_availabilities", uri.copy(query = query), extraHeaders:_*)
  override def list(query: Query, extraHeaders: Header*): F[List[IpAvailability]] =
    list[IpAvailability]("network_ip_availabilities", uri.copy(query = query), extraHeaders:_*)
  
  /**
    * Shows network IP availability details for a network.
    * @param networkId The UUID of the network.
    */
  def show(networkId: String): F[IpAvailability] = super.get(wrappedAt = Some(name), uri / networkId)
}