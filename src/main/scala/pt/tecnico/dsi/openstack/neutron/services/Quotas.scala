package pt.tecnico.dsi.openstack.neutron.services

import fs2.Stream
import cats.effect.Sync
import io.circe.{Decoder, Encoder, HCursor}
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.neutron.models.Quota
import pt.tecnico.dsi.openstack.common.services.Service

final class Quotas[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends Service[F](authToken) {
  val uri: Uri = baseUri / "quotas"
  val name = "quota"

  /** Lists quotas for projects with non-default quota values. */
  def list: Stream[F, (String, Quota)] = {
    val decoder: Decoder[(String, Quota)] = (cursor: HCursor) => for {
      projectId <- cursor.get[String]("project_id")
      quota <- cursor.as[Quota]
    } yield (projectId, quota)
    super.list(wrappedAt = s"${name}s", uri, Query.empty)(decoder)
  }
  
  /**
    * Shows quotas for a project.
    * @param projectId The UUID of the project.
    */
  def get(projectId: String): F[Quota] = super.get(wrappedAt = Some(name), uri / projectId)

  /**
    * Gets default quotas for a project.
    * @param projectId The UUID of the project.
    */
  def getDefaults(projectId: String): F[Quota] = super.get(wrappedAt = Some(name), uri / projectId / "default")

  /**
    * Updates quotas for a project.
    * @param projectId The UUID of the project.
    */
  def update(projectId: String, quotas: Quota.Update)(implicit encoder: Encoder[Quota.Update]): F[Quota] =
    super.put(wrappedAt = Some(name), quotas, uri / projectId)

  /**
   * Resets quotas to default values for a project.
   * @param projectId The UUID of the project.
   */
  def delete(projectId: String): F[Unit] = super.delete(uri / projectId)
}