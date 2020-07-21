package pt.tecnico.dsi.neutron

import cats.effect.IO
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.neutron.models.{Network, Subnet}
import pt.tecnico.dsi.neutron.services.BulkCreate

class SubnetsSpec extends CrudSpec[Subnet]("subnet", _.subnets) with BulkCreateSpec[Subnet] {

  val bulkService: NeutronClient[IO] => BulkCreate[IO, Subnet] = _.subnets
  val updateStub: IO[Subnet.Update] = IO { Subnet.Update(name = Some("cool-port")) }
  val createStub: IO[Subnet.Create] = for {
    neutron <- client
    net <- neutron.networks.create(Network.Create())
  } yield Subnet.Create(name = Some("hello"), networkId = net.id, cidr = "192.168.199.0/24", ipVersion = 4)

  override def updateComparator(read: Subnet#Read, update: Subnet#Update): Assertion =
    read.name shouldBe update.name.value
}
