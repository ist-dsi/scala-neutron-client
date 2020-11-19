package pt.tecnico.dsi.openstack.neutron

import cats.effect.IO
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.openstack.neutron.models.SecurityGroup
import pt.tecnico.dsi.openstack.neutron.services.SecurityGroups

final class SecurityGroupsSpec extends CrudSpec[SecurityGroup, SecurityGroup.Create, SecurityGroup.Update]("security group") {
  override val service: SecurityGroups[IO] = neutron.securityGroups
  
  override def createStub(name: String): SecurityGroup.Create = SecurityGroup.Create(
    name,
    project.id,
    "a description",
  )
  override def compareCreate(create: SecurityGroup.Create, model: SecurityGroup): Assertion = {
    model.name shouldBe create.name
    model.projectId shouldBe create.projectId
    model.description shouldBe create.description
  }
  
  override val updateStub: SecurityGroup.Update = SecurityGroup.Update(
    name = Some(randomName()),
    Some("a better and improved description"),
  )
  override def compareUpdate(update: SecurityGroup.Update, model: SecurityGroup): Assertion = {
    model.name shouldBe update.name.value
    model.description shouldBe update.description.value
  }
}
