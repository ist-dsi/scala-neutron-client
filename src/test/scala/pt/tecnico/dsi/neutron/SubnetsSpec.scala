package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.{Network, Port, Subnet}
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}
import pt.tecnico.dsi.openstack.common.models.WithId

class SubnetsSpec extends CrudSpec[Subnet]("subnet", _.subnets) with BulkCreateSpec[Subnet] { self =>

  val bulkService: NeutronClient[IO] => BulkCreate[IO, Subnet] = _.subnets
  val updateStub: IO[Subnet.Update] = IO { Subnet.Update(name = Some("cool-port")) }

  override def updateComparator(read: Subnet#Read, update: Subnet#Update): Assertion =
    read.name shouldBe update.name.value

  override val withStubCreated: Resource[IO, (WithId[Subnet.Read], CrudService[IO, Subnet])] =
    withNetworkCreated.flatMap { case (network, service) =>
      val create = for {
        neutron <- client
        crudService = self.service(neutron)
        createdStub <- crudService.create {
          Subnet.Create(name = Some("hello"), networkId = network.id, cidr = "192.168.199.0/24", ipVersion = 4)
        }
      } yield (createdStub, crudService)
      Resource.make(create) { case (read, service) => service.delete(read.id) }
    }
}
