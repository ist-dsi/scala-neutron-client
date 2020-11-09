package pt.tecnico.dsi.openstack.neutron

import cats.effect.{IO, Resource}
import com.comcast.ip4s._
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.openstack.neutron.models.{FloatingIp, Network, Quota, Subnet}
import pt.tecnico.dsi.openstack.neutron.services.FloatingIps

final class FloatingIpsSpec extends CrudSpec[FloatingIp[IpAddress], FloatingIp.Create[IpAddress], FloatingIp.Update[IpAddress]]("floatingIP") {
  // The domain cannot belong to a project otherwise we will get a forbidden.
  // By using the project.name (which is random) we ensure the domain does not belong to any project.
  val dnsDomain = s"${project.name}.pt."
  // This way we use the same Network,Subnet for every test, and make the logs smaller and easier to debug.
  val stubsResource: Resource[IO, (Network, Subnet[IpAddress])] = for {
    network <- resourceCreator(neutron.networks)(name => Network.Create(
      name = name,
      projectId = Some(project.id),
      // Floating IPs can only be created external networks.
      routerExternal = Some(true),
    ))
    subnet <- resourceCreator(neutron.subnets)(name => Subnet.Create(
      name = name,
      networkId = network.id,
      cidr = Some(ip"192.169.1.0" / 24),
      projectId = Some(project.id),
    ))
  } yield (network, subnet)
  
  val ((network, subnet), deletes) = stubsResource.allocated.unsafeRunSync()
  neutron.quotas.update(project.id, Quota.Update(floatingip = Some(10))).unsafeRunSync()
  override protected def afterAll(): Unit = {
    deletes.unsafeRunSync()
    super.afterAll()
  }
  
  override val service: FloatingIps[IO] = neutron.floatingIps
  
  override def createStub(name: String): FloatingIp.Create[IpAddress] = FloatingIp.Create(
    floatingNetworkId = network.id,
    description = Some("a description"),
    projectId = Some(project.id),
    subnetId = Some(subnet.id),
    dnsName = Some(name),
    dnsDomain = Some(dnsDomain),
  )
  
  override def compareCreate(create: FloatingIp.Create[IpAddress], model: FloatingIp[IpAddress]): Assertion = {
    model.floatingNetworkId shouldBe create.floatingNetworkId
    model.description shouldBe create.description
    model.projectId shouldBe create.projectId.value
    model.dnsName shouldBe create.dnsName
    model.dnsDomain shouldBe create.dnsDomain
  }
  
  override val updateStub: FloatingIp.Update[IpAddress] = FloatingIp.Update(description = Some("a better and improved description"))
  override def compareUpdate(update: FloatingIp.Update[IpAddress], model: FloatingIp[IpAddress]): Assertion = {
    model.description shouldBe update.description
  }
}
