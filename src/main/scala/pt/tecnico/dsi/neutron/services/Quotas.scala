package pt.tecnico.dsi.neutron.services

import cats.effect.Sync
import io.circe.Encoder
import org.http4s.Method.PUT
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.neutron.models.{Quota, QuotaUsage, WithId}

final class Quotas[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends Service[F](authToken) {
  import dsl._

  val uri: Uri = baseUri / "os-quota-sets"
  val name = "quota_set"

  /**
    * Shows quotas for a project.
    * @param projectId The UUID of the project.
    */
  def get(projectId: String): F[WithId[Quota]] = super.get(uri / projectId, wrappedAt = Some(name))

  /**
    * Shows quota usage for a project.
    * @param projectId The UUID of the project.
    */
  def getUsage(projectId: String): F[WithId[QuotaUsage]] = super.get(uri / projectId +?("usage", true), wrappedAt = Some(name))

  /**
    * Gets default quotas for a project.
    * @param projectId The UUID of the project.
    */
  def getDefaults(projectId: String): F[WithId[Quota]] = super.get(uri / projectId / "defaults", wrappedAt = Some(name))

  /**
    * Updates quotas for a project.
    * @param projectId The UUID of the project.
    */
  def update(projectId: String, quotas: Quota.Update)(implicit encoder: Encoder[Quota.Update]): F[Quota] =
    super.put(quotas, uri / projectId, wrappedAt = Some(name))

  /**
    * Deletes quotas for a project so the quotas revert to default values.
    * @param projectId The UUID of the project.
    */
  def delete(projectId: String): F[Unit] = super.delete(uri / projectId)
}