package pt.tecnico.dsi.openstack.neutron

import scala.util.Try
import cats.effect.{IO, Resource}
import cats.implicits._
import com.comcast.ip4s._
import org.http4s.Query
import org.http4s.client.UnexpectedStatus
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import org.scalatest.EitherValues._
import pt.tecnico.dsi.openstack.neutron.models.{SecurityGroup, SecurityGroupRule}

final class SecurityGroupRulesSpec extends Utils {
  val stubSecurityGroup: Resource[IO, SecurityGroup] = resourceCreator(neutron.securityGroups)(name => SecurityGroup.Create(name, project.id))
  // This way we use the same SecurityGroup for every test, and make the logs smaller and easier to debug.
  val (securityGroup, securityGroupDelete) = stubSecurityGroup.allocated.unsafeRunSync()
  override protected def afterAll(): Unit = {
    securityGroupDelete.unsafeRunSync()
    super.afterAll()
  }
  import neutron.securityGroupRules
  
  val createStub: SecurityGroupRule.Create = SecurityGroupRule.Create(5000, ip"192.168.1.1" / 24)(securityGroup.id)
  def compareCreate(create: SecurityGroupRule.Create, model: SecurityGroupRule): Assertion = {
    model.description.value shouldBe create.description
    model.securityGroupId shouldBe create.securityGroupId
    model.direction shouldBe create.direction
    model.ipVersion shouldBe create.ipVersion
    model.protocol shouldBe create.protocol
    model.portRangeMin shouldBe create.portRangeMin
    model.portRangeMax shouldBe create.portRangeMax
    model.remote shouldBe create.remote
  }
  
  val resource: Resource[IO, SecurityGroupRule] = Resource.make(securityGroupRules.create(createStub))(model => securityGroupRules.delete(model.id))
  
  "Security Group Rules service" should {
    s"list security group rules" in resource.use[IO, Assertion] { model =>
      securityGroupRules.list().compile.toList.idempotently { models =>
        models.exists(m => Try(m shouldBe model).isSuccess) shouldBe true
      }
    }
    
    s"create security group rules" in {
      val repetitions = 3
      for {
        _ <- securityGroupRules.create(createStub).idempotently(compareCreate(createStub, _), repetitions)
        list <- securityGroupRules.list(Query.fromPairs(
          "security_group_id" -> securityGroup.id,
          "direction" -> createStub.direction.toString.toLowerCase,
          "ethertype" -> createStub.ipVersion.toString,
          "remote_ip_prefix" -> createStub.remote.value.left.value.toString,
          "port_range_min" -> createStub.portRangeMin.value.toString,
          "port_range_max" -> createStub.portRangeMax.value.toString,
          "limit" -> repetitions.toString,
        )).compile.toList
        _ <- list.parTraverse_(securityGroupRules.delete)
      } yield list.size shouldBe 1
    }
    
    s"get security group rules (existing id)" in resource.use[IO, Assertion] { model =>
      securityGroupRules.get(model.id).idempotently(_.value shouldBe model)
    }
    s"get security group rules (non-existing id)" in {
      securityGroupRules.get("non-existing-id").idempotently(_ shouldBe None)
    }
    
    s"apply security group rules (existing id)" in resource.use[IO, Assertion] { model =>
      securityGroupRules.apply(model.id).idempotently(_ shouldBe model)
    }
    s"apply security group rules (non-existing id)" in {
      securityGroupRules.apply("non-existing-id").attempt.idempotently(_.left.value shouldBe a [UnexpectedStatus])
    }
    
    s"delete a security group rules" in resource.use[IO, Assertion] { model =>
      securityGroupRules.delete(model.id).idempotently(_ shouldBe ())
    }
  }
}
