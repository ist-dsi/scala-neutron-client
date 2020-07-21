package pt.tecnico.dsi.neutron

import cats.effect.IO
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.Network
import pt.tecnico.dsi.neutron.services.BulkCreate

class NetworksSpec extends CrudSpec[Network]("network", _.networks) with BulkCreateSpec[Network] {

  val bulkService: NeutronClient[IO] => BulkCreate[IO, Network] = _.networks
  val createStub: IO[Network.Create] = IO { Network.Create() }
  val updateStub: IO[Network.Update] = IO { Network.Update(name = Some("port-name")) }

  override def updateComparator(read: Network#Read, update: Network#Update): Assertion =
    read.name shouldBe update.name.value
}
