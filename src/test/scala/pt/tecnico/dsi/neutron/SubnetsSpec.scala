package pt.tecnico.dsi.neutron

import cats.effect.IO
import pt.tecnico.dsi.neutron.models.{Network, Subnet}
import pt.tecnico.dsi.neutron.services.BulkCreate

class SubnetsSpec extends CrudSpec[Subnet]("subnet", _.subnets) with BulkCreateSpec[Subnet] {

  val bulkService: NeutronClient[IO] => BulkCreate[IO, Subnet] = _.subnets
  val updateStub: Subnet.Update = Subnet.Update(name = Some("cool-port"))
  val createStub: IO[Subnet.Create] = for {
    neutron <- client
    net <- neutron.networks.create(Network.Create())
  } yield Subnet.Create(name = Some("hello"), networkId = net.id, cidr = "192.168.199.0/24", ipVersion = 4)

  "Subnets service" should {
    "update" in {
      for {
        neutron <- client
        stub <- createStub
        port <- neutron.subnets.create(stub)
        updated <- neutron.subnets.update(port.id, updateStub)
      } yield assert {
        updated.name == updateStub.name.get
      }
    }
  }
}
