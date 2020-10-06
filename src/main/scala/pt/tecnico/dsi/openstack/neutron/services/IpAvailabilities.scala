package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Query, Uri}
import pt.tecnico.dsi.openstack.common.services.Service
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.IpAvailability

final class IpAvailabilities[F[_]: Sync: Client](baseUri: Uri, session: Session) extends Service[F](session.authToken) {
  val uri: Uri = baseUri / "network-ip-availabilities"
  val name = "network_ip_availability"

  /** Lists network IP availability of all networks. */
  def stream(query: Query = Query.empty): Stream[F, IpAvailability] =
    super.stream[IpAvailability](wrappedAt = "network_ip_availabilities", uri, query)
  
  /**
    * Shows network IP availability details for a network.
    * @param networkId The UUID of the network.
    */
  def show(networkId: String): F[IpAvailability] = super.get(wrappedAt = Some(name), uri / networkId)
}