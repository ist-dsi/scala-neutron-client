package pt.tecnico.dsi.openstack.neutron

import cats.effect.{IO, Resource}
import com.comcast.ip4s._
import org.http4s.Query
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.neutron.models.{IpAvailability, Network, RichCidr, RichIp, Subnet}

final class IpAvailabilitiesSpec extends Utils {
  val resource: Resource[IO, (Network, Subnet[IpAddress])] = for {
    network <- resourceCreator(neutron.networks)(name => Network.Create(name = name, projectId = Some(project.id)))
    subnet <- resourceCreator(neutron.subnets)(name => Subnet.Create(
      name = name,
      networkId = network.id,
      cidr = Some(ip"192.169.1.0" / 24),
      projectId = Some(project.id),
    ))
  } yield (network, subnet)
  
  def testAvailability(network: Network, subnet: Subnet[IpAddress], availability: IpAvailability): Assertion = {
    availability.networkId shouldBe network.id
    availability.networkName shouldBe network.name
    availability.projectId shouldBe project.id
    // -3 because the network, broadcast, and gateway ips don't count as IPs, because logic </sarcasm>.
    val totalIps = subnet.cidr.totalIps - 3
    availability.totalIps shouldBe totalIps
    availability.usedIps shouldBe 0 // As everyone knows the gateway does not use an IP </sarcasm>.
    availability.subnetIpAvailability.length shouldBe 1
    val subnetAvailability = availability.subnetIpAvailability.head
    subnetAvailability.cidr shouldBe subnet.cidr
    subnetAvailability.ipVersion shouldBe subnet.cidr.address.version
    subnetAvailability.subnetId shouldBe subnet.id
    subnetAvailability.subnetId shouldBe subnet.id
    subnetAvailability.subnetName shouldBe subnet.name
    subnetAvailability.totalIps shouldBe totalIps
    subnetAvailability.usedIps shouldBe 0
  }
  
  "The ip availabilities service" should {
    "show the ip avalabilities of a network" in resource.use[IO, Assertion] { case (network, subnet) =>
      neutron.ipAvailabilities.show(network.id).idempotently(testAvailability(network, subnet, _))
    }
    "list the ip avalabilities" in resource.use[IO, Assertion] { case (network, subnet) =>
      // Very devious way of testing the list. However if we listed all the availabilities we would be making
      // the test code very dependent on the existing networks of the VM we are testing against
      neutron.ipAvailabilities.stream(Query.fromPairs("network_id" -> network.id)).compile.toList.idempotently { availabilities =>
        availabilities.length shouldBe 1
        testAvailability(network, subnet, availabilities.head)
      }
    }
  }
}
