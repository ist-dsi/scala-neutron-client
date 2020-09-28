package pt.tecnico.dsi.openstack.neutron

/*
import cats.effect.IO
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.openstack.neutron.models.Network
import pt.tecnico.dsi.openstack.neutron.services.{Networks}

final class NetworksSpec extends CrudSpec[Network, Network.Create, Network.Update]("network") /*with BulkCreateSpec[Network, Network.Create]*/ {
  override val service: Networks[IO] = neutron.networks
  
  override def createStub(name: String): Network.Create = Network.Create(
    Some(project.id),
    name,
    "a description",
    
  )
  override def compareCreate(create: Network.Create, model: Network): Assertion = {
    model.name shouldBe create.name
    model.description shouldBe create.description
    // Since we didn't specified the domainId, and the token we used to authenticate isn't domain-scoped
    // the group will be created with domainId = default
    model.domainId shouldBe domainIdFromScope(keystone.session.scope)
  }
  
  override def updateStub: Network.Update = Network.Update(name = Some(randomName()), Some("a better and improved description"))
  override def compareUpdate(update: Network.Update, model: Network): Assertion = {
    model.name shouldBe update.name.value
    model.description shouldBe update.description
  }
}
*/