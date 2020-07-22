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
  val updateStub: IO[Port.Update] = withRandomName { name => IO { Port.Update(name = Some(name)) } }

  override val withStubCreated: Resource[IO, WithId[Port#Read]] = withNetworkCreated.flatMap { network =>
    val stub = withRandomName { name =>
      service.create {
        Port.Create(name = Some(name), networkId = network.id)
      }
    }
    Resource.make(stub) { stub => service.delete(stub.id) }
  }

  override def updateComparator(read: Port#Read, update: Port#Update): Assertion =
    read.name shouldBe update.name.value

  override def withBulkCreated(n: Int): Resource[IO, List[WithId[Port#Read]]] = withNetworkCreated.flatMap { network =>
    val created = withRandomName { name => client.ports.create {
      (1 to n).map { x =>
          Port.Create(name = Some(s"$name$x"), networkId = network.id)
        }.toList
      }
    }
    Resource.make(created)(_.traverse_(stub => service.delete(stub.id)))
  }
}
