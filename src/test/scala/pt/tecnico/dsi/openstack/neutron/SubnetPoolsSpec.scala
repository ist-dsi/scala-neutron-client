package pt.tecnico.dsi.openstack.neutron

import scala.util.Random
import cats.effect.IO
import com.comcast.ip4s.Ipv4Address
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.openstack.neutron.models.SubnetPool
import pt.tecnico.dsi.openstack.neutron.services.SubnetPools

final class SubnetPoolsSpec extends CrudSpec[SubnetPool, SubnetPool.Create, SubnetPool.Update]("security group") {
  override val service: SubnetPools[IO] = neutron.subnetPools
  
  override def createStub(name: String): SubnetPool.Create = SubnetPool.Create(
    name,
    "a description",
    Some(project.id),
    List(Ipv4Address.fromBytes(192, 168, Random.between(1, 250), 0) / 24),
  )
  override def compareCreate(create: SubnetPool.Create, model: SubnetPool): Assertion = {
    model.name shouldBe create.name
    model.projectId shouldBe create.projectId.value
    model.description shouldBe create.description
    model.prefixes shouldBe create.prefixes
  }
  
  override val updateStub: SubnetPool.Update = SubnetPool.Update(
    name = Some(randomName()),
    Some("a better and improved description"),
  )
  override def compareUpdate(update: SubnetPool.Update, model: SubnetPool): Assertion = {
    model.name shouldBe update.name.value
    model.description shouldBe update.description.value
  }
}
