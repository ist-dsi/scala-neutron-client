package pt.tecnico.dsi.neutron

import cats.effect.IO
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.Network
import pt.tecnico.dsi.neutron.services.BulkCreate

class NetworksSpec extends CrudSpec[Network]("network", _.networks) with BulkCreateSpec[Network] {

  val bulkService: NeutronClient[IO] => BulkCreate[IO, Network] = _.networks
  val createStub: IO[Network.Create] = IO { Network.Create() }
  val updateStub: Network.Update = Network.Update(name = Some("port-name"))

  "Networks service" should {
    "update" in {
      for {
        (stub, service) <- withSubCreated
        updated <- service.update(stub.id, updateStub)
      } yield updated.name shouldBe updateStub.name.value
    }
  }
}
