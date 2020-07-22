package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import cats.implicits._
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.Subnet
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}
import pt.tecnico.dsi.openstack.common.models.WithId

class SubnetsSpec extends CrudSpec[Subnet]("subnet") with BulkCreateSpec[Subnet] { self =>

  val service: CrudService[IO, Subnet] with BulkCreate[IO, Subnet] = client.subnets
  val bulkService: NeutronClient[IO] => BulkCreate[IO, Subnet] = _.subnets
  val updateStub: IO[Subnet.Update] = IO { Subnet.Update(name = Some("cool-port")) }

  override def updateComparator(read: Subnet#Read, update: Subnet#Update): Assertion =
    read.name shouldBe update.name.value

  override val withStubCreated: Resource[IO, WithId[Subnet.Read]] =
    withNetworkCreated.flatMap { network =>
      val create = service.create {
        Subnet.Create(name = Some("hello"), networkId = network.id, cidr = "192.168.199.0/24", ipVersion = 4)
      }
      Resource.make(create) { stub => service.delete(stub.id) }
    }

  override def withBulkCreated(n: Int): Resource[IO, List[WithId[Subnet#Read]]] = withNetworkCreated.flatMap { network =>
    val created = client.subnets.create { (1 to n).map { x =>
        Subnet.Create(name = Some("hello"), networkId = network.id, cidr = s"192.168.199.0/${x}", ipVersion = 4)
      }.toList
    }
    Resource.make(created)(_.traverse_(y => service.delete(y.id)))
  }

}
