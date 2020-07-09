package pt.tecnico.dsi.neutron

import cats.effect.IO
import pt.tecnico.dsi.neutron.models.Network
import pt.tecnico.dsi.neutron.services.BulkCreate

class NetworksSpec extends CrudSpec[Network]("network", _.networks) with BulkCreateSpec[Network] {

  val bulkService: NeutronClient[IO] => BulkCreate[IO, Network] = _.networks

  val createStub: IO[Network.Create] = IO { Network.Create() }
  val updateStub: IO[Network.Update] = IO {
    Network.Update(
      name = Some("port-name")
    )
  }

  "Networks service" should {
    "update" in {
      for {
        (stub, service) <- withSubCreated
        ustub <- updateStub
        updated <- service.update(stub.id, ustub)
      } yield assert(updated.name == ustub.name.get)
    }
  }

}
