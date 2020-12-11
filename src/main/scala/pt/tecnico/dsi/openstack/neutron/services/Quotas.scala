package pt.tecnico.dsi.openstack.neutron.services

import cats.effect.Sync
import fs2.Stream
import io.circe.{Decoder, Encoder, HCursor}
import org.http4s.Uri
import org.http4s.client.Client
import pt.tecnico.dsi.openstack.common.services.Service
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.neutron.models.{Quota, QuotaUsage}

final class Quotas[F[_]: Sync: Client](baseUri: Uri, session: Session) extends Service[F](baseUri, "quota", session.authToken) {
  private val wrappedAt: Option[String] = Some(name)
  
  /** Streams quotas for projects with non-default quota values. */
  def stream: Stream[F, (String, Quota)] = {
    val decoder: Decoder[(String, Quota)] = (cursor: HCursor) => for {
      projectId <- cursor.get[String]("project_id")
      quota <- cursor.as[Quota]
    } yield (projectId, quota)
    super.stream(wrappedAt = pluralName, uri)(decoder)
  }
  
  /** Lists quotas for projects with non-default quota values. */
  def list: F[List[(String, Quota)]] = stream.compile.toList
  
  /**
   * Shows quotas for a project.
   * Neutron always returns a Quota even if the project does not exist. That is why there is no method called `get`.
   * @param projectId The UUID of the project.
   */
  def apply(projectId: String): F[Quota] = super.get(wrappedAt, uri / projectId)
  
  /**
   * Shows quota usage for a project.
   * Neutron always returns a Quota even if the project does not exist. That is why there is no method called `getUsage`.
   * @param projectId The UUID of the project.
   */
  def applyUsage(projectId: String): F[QuotaUsage] = super.get(wrappedAt, uri / projectId / "details.json")
  
  /**
   * Gets default quotas for a project.
   * Neutron always returns a Quota even if the project does not exist. That is why there is no method called `getDefaults`.
   * @param projectId The UUID of the project.
   */
  def applyDefaults(projectId: String): F[Quota] = super.get(wrappedAt, uri / projectId / "default")
  
  /**
    * Updates quotas for a project.
    * @param projectId The UUID of the project.
    */
  def update(projectId: String, quotas: Quota.Update)(implicit encoder: Encoder[Quota.Update]): F[Quota] = {
    // Partial updates are done with a put, everyone knows that </sarcasm>
    super.put(wrappedAt = Some(name), quotas, uri / projectId)
  }
  
  /**
   * Resets quotas to default values for a project.
   * @param projectId The UUID of the project.
   */
  def delete(projectId: String): F[Unit] = super.delete(uri / projectId)
}