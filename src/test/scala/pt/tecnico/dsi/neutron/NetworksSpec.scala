package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import cats.implicits._
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.Network
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}

class NetworksSpec extends CrudSpec[Network]("network") with BulkCreateSpec[Network] {

  val service: CrudService[IO, Network] with BulkCreate[IO, Network] = neutron.networks
  val updateStub: IO[Network.Update] = withRandomName { name => IO { Network.Update(name = Some(name)) } }

  override def updateComparator(read: Network#Read, update: Network#Update): Assertion =
    read.name shouldBe update.name.value

  override val withStubCreated: Resource[IO, Network.Read] = withNetworkCreated
  override def withBulkCreated(n: Int): Resource[IO, List[Network.Read]] = {
    val created = neutron.networks.create { List.fill(n)(Network.Create()) }
    Resource.make(created)(_.traverse_(stub => service.delete(stub.id)))
  }

}
