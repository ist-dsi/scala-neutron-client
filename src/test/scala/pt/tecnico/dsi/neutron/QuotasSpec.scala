package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import org.scalatest.Assertion
import pt.tecnico.dsi.neutron.models.Quota
import pt.tecnico.dsi.openstack.keystone.models.Project

class QuotasSpec extends Utils {

  val withStubProject: Resource[IO, String] = {
    Resource.make(keystone.projects.create(
      Project.Create("dummy", Some("dummy project"), Some("default"))
    ).map(_.id))(x => keystone.projects.delete(x))
  }

  // These are the default quotas for the Neutron we are testing against
  val defaultQuota: Quota = Quota(
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
    "list quotas" in withStubProject.use[IO, Assertion] { dummyProjectId =>
      for {
        // Ensure there is at least one project with non-default quotas
        _ <- neutron.quotas.update(dummyProjectId, Quota.Update(network = Some(30)))
        quotas <- neutron.quotas.list.map(_._1).compile.toList
        // Reset the quotas back to default values to ensure we can run this suite multiple times
        _ <- neutron.quotas.delete(dummyProjectId)
      } yield quotas should contain(dummyProjectId)
    }

    "get default quotas for a project" in withStubProject.use[IO, Assertion] { dummyProjectId =>
      neutron.quotas.getDefaults(dummyProjectId).idempotently(_ shouldBe defaultQuota)
    }

    "get quotas for a project" in withStubProject.use[IO, Assertion] { dummyProjectId =>
      neutron.quotas.get(dummyProjectId).idempotently(_ shouldBe defaultQuota)
    }
    "update quotas for a project" in withStubProject.use[IO, Assertion]  { dummyProjectId =>
      val newQuotas = Quota.Update(floatingip = Some(25), router = Some(25))
      neutron.quotas.update(dummyProjectId, newQuotas).idempotently { quota =>
        quota.floatingip shouldBe 25
        quota.router shouldBe 25
      }
    }
    "delete quotas for a project" in withStubProject.use[IO, Assertion] { dummyProjectId =>
      neutron.quotas.delete(dummyProjectId).idempotently(_ shouldBe ())
    }
  }
}
