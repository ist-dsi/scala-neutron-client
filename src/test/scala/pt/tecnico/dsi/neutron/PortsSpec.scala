package pt.tecnico.dsi.neutron

import cats.effect.IO
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.{Network, Port}
import pt.tecnico.dsi.neutron.services.BulkCreate

class PortsSpec extends CrudSpec[Port]("port", _.ports) with BulkCreateSpec[Port] {

  val bulkService: NeutronClient[IO] => BulkCreate[IO, Port] = _.ports
  val updateStub: Port.Update = Port.Update(name = Some("cool-port"))
  val createStub: IO[Port.Create] = for {
    neutron <- client
    net <- neutron.networks.create(Network.Create())
  } yield Port.Create(name = Some("hello"), networkId = net.id)

  "Ports service" should {
    "update" in {
      for {
        neutron <- client
        stub <- createStub
        port <- neutron.ports.create(stub)
        updated <- neutron.ports.update(port.id, updateStub)
      } yield updated.name shouldBe updateStub.name.value
    }
  }
}
