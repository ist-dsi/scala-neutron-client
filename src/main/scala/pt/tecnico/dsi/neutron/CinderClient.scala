package pt.tecnico.dsi.neutron

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.services._

class CinderClient[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
	val uri: Uri = if (baseUri.path.endsWith("v3") || baseUri.path.endsWith("v3/")) baseUri else baseUri / "v3"

	def quotas(adminProjectId: String) = new Quotas[F](uri / adminProjectId, authToken)
	def volumes(projectId: String) = new Volumes[F](uri / projectId, authToken)
}
