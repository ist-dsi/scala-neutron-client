package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.Network
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}
import pt.tecnico.dsi.openstack.common.models.WithId

class NetworksSpec extends CrudSpec[Network]("network") with BulkCreateSpec[Network] {

  val service: CrudService[IO, Network] with BulkCreate[IO, Network] = client.networks
  val bulkService: NeutronClient[IO] => BulkCreate[IO, Network] = _.networks
  val updateStub: IO[Network.Update] = IO { Network.Update(name = Some("port-name")) }

  override def updateComparator(read: Network#Read, update: Network#Update): Assertion =
    read.name shouldBe update.name.value

  override val withStubCreated: Resource[IO, WithId[Network.Read]] = withNetworkCreated
  override val withBulkCreated: Resource[IO, List[WithId[Network.Read]]] = _
}
