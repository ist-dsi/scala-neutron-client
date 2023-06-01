package pt.tecnico.dsi.openstack.neutron

import cats.effect.{IO, Resource}
import pt.tecnico.dsi.openstack.common.models.Usage
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.neutron.models.{Quota, QuotaUsage}

class QuotasSpec extends Utils:
  val withStubProject: Resource[IO, Project] = resourceCreator(keystone.projects)(Project.Create(_))
  
  // These are the default quotas for the Neutron we are testing against
  val defaultQuotas: Quota = Quota(
    floatingip = 0,
    network = 100,
    port = 500,
    rbacPolicy = 10,
    router = 10,
    securityGroupRule = 100,
    securityGroup = 10,
    subnet = 100,
    subnetpool = -1,
  )
  val defaultUsageQuotas = QuotaUsage(
    floatingip = Usage(0, defaultQuotas.floatingip, 0),
    network = Usage(0, defaultQuotas.network, 0),
    port = Usage(0, defaultQuotas.port, 0),
    rbacPolicy = Usage(0, defaultQuotas.rbacPolicy, 0),
    router = Usage(0, defaultQuotas.router, 0),
    securityGroupRule = Usage(0, defaultQuotas.securityGroupRule, 0),
    securityGroup = Usage(0, defaultQuotas.securityGroup, 0),
    subnet = Usage(0, defaultQuotas.subnet, 0),
    subnetpool = Usage(0, defaultQuotas.subnetpool, 0),
  )
  
  "Quotas service" should {
    "list quotas" in withStubProject.use { project =>
      for
      // Ensure there is at least one project with non-default quotas
        _ <- neutron.quotas.update(project.id, Quota.Update(network = Some(30)))
        quotas <- neutron.quotas.stream.map(_._1).compile.toList
        // Reset the quotas back to default values to ensure we can run this suite multiple times
        _ <- neutron.quotas.delete(project.id)
      yield quotas should contain(project.id)
    }

    "apply quotas for a project (existing id)" in withStubProject.use { project =>
      neutron.quotas.apply(project.id).idempotently(_ shouldBe defaultQuotas)
    }
    "apply quotas for a project (non-existing id)" in :
      // This is not a mistake in the test. Neutron does return a Quota even if the project does not exist :faceplam:
      neutron.quotas.apply("non-existing-id").idempotently(_ shouldBe defaultQuotas)

    "apply usage quotas for a project (existing id)" in withStubProject.use { project =>
      neutron.quotas.applyUsage(project.id).idempotently(_ shouldBe defaultUsageQuotas)
    }
    "apply usage quotas for a project (non-existing id)" in :
      // This is not a mistake in the test. Neutron does return a Quota even if the project does not exist :faceplam:
      neutron.quotas.applyUsage("non-existing-id").idempotently(_ shouldBe defaultUsageQuotas)

    "apply default quotas for a project (existing id)" in withStubProject.use { project =>
      neutron.quotas.applyDefaults(project.id).idempotently(_ shouldBe defaultQuotas)
    }
    "apply default quotas for a project (non-existing id)" in :
      // This is not a mistake in the test. Neutron does return a Quota even if the project does not exist :faceplam:
      neutron.quotas.applyDefaults("non-existing-id").idempotently(_ shouldBe defaultQuotas)

    "update quotas for a project" in withStubProject.use { project =>
      val newQuotas = Quota.Update(floatingip = Some(25), router = Some(25))
      neutron.quotas.update(project.id, newQuotas).idempotently { quota =>
        quota.floatingip shouldBe 25
        quota.router shouldBe 25
      }
    }
    "delete quotas for a project" in withStubProject.use { project =>
      neutron.quotas.delete(project.id).idempotently(_ shouldBe())
    }
  }
