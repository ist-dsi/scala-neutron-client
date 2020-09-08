package pt.tecnico.dsi.openstack.neutron

import cats.effect.{IO, Resource}
import cats.implicits._
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.openstack.neutron.models.Port
import pt.tecnico.dsi.openstack.neutron.services.{BulkCreate, CrudService}

class PortsSpec extends CrudSpec[Port]("port") with BulkCreateSpec[Port] {

  val service: CrudService[IO, Port] with BulkCreate[IO, Port] = neutron.ports
  val updateStub: IO[Port.Update] = withRandomName { name => IO { Port.Update(name = Some(name)) } }

  override val withStubCreated: Resource[IO, Port#Read] = withNetworkCreated.flatMap { network =>
    val stub = withRandomName { name =>
      service.create {
        Port.Create(name = Some(name), networkId = network.id)
      }
    }
    Resource.make(stub) { stub => service.delete(stub.id) }
  }

  override def updateComparator(read: Port#Read, update: Port#Update): Assertion =
    read.name shouldBe update.name.value

  override def withBulkCreated(n: Int): Resource[IO, List[Port#Read]] = withNetworkCreated.flatMap { network =>
    val created = withRandomName { name =>
      val ports = List.tabulate(n)(i => Port.Create(name = Some(s"$name$i"), networkId = network.id))
      neutron.ports.create(ports)
    }
    Resource.make(created)(_.traverse_(stub => service.delete(stub.id)))
  }
}
