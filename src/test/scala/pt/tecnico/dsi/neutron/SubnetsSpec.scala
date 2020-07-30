package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import cats.implicits._
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.Subnet
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}

class SubnetsSpec extends CrudSpec[Subnet]("subnet") with BulkCreateSpec[Subnet] { self =>

  val service: CrudService[IO, Subnet] with BulkCreate[IO, Subnet] = neutron.subnets
  val bulkService: NeutronClient[IO] => BulkCreate[IO, Subnet] = _.subnets
  val updateStub: IO[Subnet.Update] = withRandomName { name => IO { Subnet.Update(name = Some(name)) } }

  override def updateComparator(read: Subnet#Read, update: Subnet#Update): Assertion =
    read.name shouldBe update.name.value

  override val withStubCreated: Resource[IO, Subnet.Read] =
    withNetworkCreated.flatMap { network =>
      val create = withRandomName { name =>
        service.create {
          Subnet.Create(name = Some(name), networkId = network.id, cidr = "192.168.199.0/24", ipVersion = 4)
        }
      }
      Resource.make(create) { stub => service.delete(stub.id) }
    }

  override def withBulkCreated(n: Int): Resource[IO, List[Subnet#Read]] = withNetworkCreated.flatMap { network =>
    val created = withRandomName { name =>
        val subnets = List.tabulate(n)(i => Subnet.Create(Some(s"$name$i"), networkId = network.id, cidr = s"192.168.$i.0/24", ipVersion = 4) )
        neutron.subnets.create(subnets)
    }
    Resource.make(created)(_.traverse_(stub => service.delete(stub.id)))
  }

}
