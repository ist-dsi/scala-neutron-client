package pt.tecnico.dsi.openstack.neutron

import scala.util.{Random, Try}
import cats.effect.{IO, Resource}
import cats.implicits._
import com.comcast.ip4s._
import org.http4s.Query
import pt.tecnico.dsi.openstack.common.models.UnexpectedStatus
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
  
  def createStub: SecurityGroupRule.Create = SecurityGroupRule.Create(
    port = 5000,
    cidr = Ipv4Address.fromBytes(192, 168, Random.between(1, 255), 0) / 24,
  )(securityGroup.id)
  def compareCreate(create: SecurityGroupRule.Create, model: SecurityGroupRule): Assertion = {
    model.securityGroupId shouldBe create.securityGroupId
    model.direction shouldBe create.direction
    model.ipVersion shouldBe create.ipVersion
    model.protocol shouldBe create.protocol
    model.portRangeMin shouldBe create.portRangeMin
    model.portRangeMax shouldBe create.portRangeMax
    model.remote shouldBe create.remote
  }
  
  def resource: Resource[IO, SecurityGroupRule] = Resource.make(securityGroupRules.create(createStub))(model => securityGroupRules.delete(model.id))
  
  "Security Group Rules service" should {
    s"list security group rules" in resource.use[IO, Assertion] { model =>
      securityGroupRules.list().idempotently { models =>
        models.exists(m => Try(m shouldBe model).isSuccess) shouldBe true
      }
    }
    
    s"create security group rules" in {
      val stub: SecurityGroupRule.Create = createStub
      val repetitions = 3
      for {
        _ <- securityGroupRules.createWithDeduplication(stub).idempotently(compareCreate(stub, _), repetitions)
        list <- securityGroupRules.list(Query.fromPairs(
          "security_group_id" -> securityGroup.id,
          "direction" -> stub.direction.toString.toLowerCase,
          "ethertype" -> stub.ipVersion.toString,
          "remote_ip_prefix" -> stub.remote.value.left.value.toString,
          "port_range_min" -> stub.portRangeMin.value.toString,
          "port_range_max" -> stub.portRangeMax.value.toString,
          "limit" -> repetitions.toString,
        ))
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
