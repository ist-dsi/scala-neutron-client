package pt.tecnico.dsi.neutron

import cats.effect.IO
import pt.tecnico.dsi.neutron.models.Quota
import pt.tecnico.dsi.openstack.keystone.models.Project

class QuotasSpec extends Utils {
  val withStubProject: IO[(NeutronClient[IO], String)] =
    for {
      keystone <- keystoneClient
      dummyProject <- keystone.projects.create(Project("dummy", "dummy project", "default"))
      neutron <- client
    } yield (neutron, dummyProject.id)

  // These are the default quotas for the Neutron we are testing against
  val defaultQuota = Quota(
    floatingip = 50,
    network = 100,
    port = 500,
    rbacPolicy = 10,
    router = 10,
    securityGroupRule = 100,
    securityGroup = 10,
    subnet = 100,
    subnetpool = -1,
  )

  "Quotas service" should {
    "list quotas" in {
      for {
        (neutron, dummyProjectId) <- withStubProject
        // Ensure there is at least one project with non-default quotas
        _ <- neutron.quotas.update(dummyProjectId, Quota.Update(network = Some(30)))
        quotas <- neutron.quotas.list.map(_._1).compile.toList
        // Reset the quotas back to default values to ensure we can run this suite multiple times
        _ <- neutron.quotas.delete(dummyProjectId)
      } yield quotas should contain(dummyProjectId)
    }
    "get default quotas for a project" in withStubProject.flatMap { case (neutron, dummyProjectId) =>
      neutron.quotas.getDefaults(dummyProjectId).idempotently(_ shouldBe defaultQuota)
    }
    "get quotas for a project" in withStubProject.flatMap { case (cinder, dummyProjectId) =>
      cinder.quotas.get(dummyProjectId).idempotently(_ shouldBe defaultQuota)
    }
    "update quotas for a project" in withStubProject.flatMap { case (cinder, dummyProjectId) =>
      val newQuotas = Quota.Update(floatingip = Some(25), router = Some(25))
      cinder.quotas.update(dummyProjectId, newQuotas).idempotently { quota =>
        quota.floatingip shouldBe 25
        quota.router shouldBe 25
      }
    }
    "delete quotas for a project" in withStubProject.flatMap { case (cinder, dummyProjectId) =>
      cinder.quotas.delete(dummyProjectId).idempotently(_ shouldBe ())
    }
  }
}
