package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import cats.implicits._
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.Port
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}
import pt.tecnico.dsi.openstack.common.models.WithId

class PortsSpec extends CrudSpec[Port]("port") with BulkCreateSpec[Port] { self =>

  val service: CrudService[IO, Port] with BulkCreate[IO, Port] = client.ports
  val bulkService: NeutronClient[IO] => BulkCreate[IO, Port] = _.ports
  val updateStub: IO[Port.Update] = IO { Port.Update(name = Some("cool-port")) }

  override val withStubCreated: Resource[IO, WithId[Port#Read]] = withNetworkCreated.flatMap { network =>
    val stub = service.create {
        Port.Create(name = Some("hello"), networkId = network.id)
    }
    Resource.make(stub) { s => service.delete(s.id) }
  }

  override def updateComparator(read: Port#Read, update: Port#Update): Assertion =
    read.name shouldBe update.name.value

  override def withBulkCreated(n: Int): Resource[IO, List[WithId[Port#Read]]] = withNetworkCreated.flatMap { network =>
    val created = client.ports.create { List.fill(n) {
      Port.Create(name = Some("hello"), networkId = network.id)
    }
    }
    Resource.make(created)(_.traverse_(y => service.delete(y.id)))
  }
}
