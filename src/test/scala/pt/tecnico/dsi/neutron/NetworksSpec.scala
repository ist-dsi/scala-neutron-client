package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.Network
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}
import pt.tecnico.dsi.openstack.common.models.WithId

class NetworksSpec extends CrudSpec[Network]("network", _.networks) with BulkCreateSpec[Network] {

  val bulkService: NeutronClient[IO] => BulkCreate[IO, Network] = _.networks
  val updateStub: IO[Network.Update] = IO { Network.Update(name = Some("port-name")) }

  override def updateComparator(read: Network#Read, update: Network#Update): Assertion =
    read.name shouldBe update.name.value

  override val withStubCreated: Resource[IO, (WithId[Network.Read], CrudService[IO, Network])] = withNetworkCreated
}
