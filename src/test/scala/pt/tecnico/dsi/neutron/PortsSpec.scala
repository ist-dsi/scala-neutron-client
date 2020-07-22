package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.{Network, Port}
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}
import pt.tecnico.dsi.openstack.common.models.WithId

class PortsSpec extends CrudSpec[Port]("port", _.ports) with BulkCreateSpec[Port] { self =>

  val bulkService: NeutronClient[IO] => BulkCreate[IO, Port] = _.ports
  val updateStub: IO[Port.Update] = IO { Port.Update(name = Some("cool-port")) }

  override val withStubCreated: Resource[IO, (WithId[Port.Read], CrudService[IO, Port])] = withNetworkCreated.flatMap { case (network, service) =>
    val create = for {
      neutron <- client
      crudService = self.service(neutron)
      createdStub <- crudService.create {
        Port.Create(name = Some("hello"), networkId = network.id)
      }
    } yield (createdStub, crudService)
    Resource.make(create) { case (read, service) => service.delete(read.id) }
  }

  override def updateComparator(read: Port#Read, update: Port#Update): Assertion =
    read.name shouldBe update.name.value

}
