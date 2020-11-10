package pt.tecnico.dsi.openstack.neutron

import scala.util.Random
import cats.effect.{IO, Resource}
import cats.implicits._
import com.comcast.ip4s._
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.openstack.neutron.models.{AllocationPool, Network, Subnet}
import pt.tecnico.dsi.openstack.neutron.services.Subnets

final class SubnetsSpec extends CrudSpec[Subnet[IpAddress], Subnet.Create[IpAddress], Subnet.Update[IpAddress]]("subnet")
  with BulkCreateSpec[Subnet[IpAddress], Subnet.Create[IpAddress]] {
  // This way we use the same Network for every test, and make the logs smaller and easier to debug.
  val stubNetwork: Resource[IO, Network] = resourceCreator(neutron.networks)(name => Network.Create(
    name = name,
    projectId = Some(project.id),
  ))
  val (network, networkDelete) = stubNetwork.allocated.unsafeRunSync()
  override protected def afterAll(): Unit = {
    networkDelete.unsafeRunSync()
    super.afterAll()
  }
  
  override val service: Subnets[IO] = neutron.subnets
  override val bulkService: Subnets[IO] = service
  
  override def createStub(name: String): Subnet.Create[IpAddress] = Subnet.Create(
    name,
    network.id,
    Some("a description"),
    Some(Ipv4Address.fromBytes(192, 168, Random.between(50, 250), 0) / 24),
    projectId = Some(project.id)
  )
  override def compareCreate(create: Subnet.Create[IpAddress], model: Subnet[IpAddress]): Assertion = {
    model.name shouldBe create.name
    model.description shouldBe create.description.value
    model.projectId shouldBe create.projectId.value
    model.networkId shouldBe create.networkId
    model.cidr shouldBe create.cidr.value
    model.gateway shouldBe create.cidr.map(_.prefix.next)
    model.allocationPools shouldBe List(AllocationPool.fromCidr(create.cidr.value))
  }
  
  override val updateStub: Subnet.Update[IpAddress] = Subnet.Update(
    name = Some(randomName()),
    Some("a better and improved description"),
    gatewayIp = Some(ip"192.168.199.10")
  )
  override def compareUpdate(update: Subnet.Update[IpAddress], model: Subnet[IpAddress]): Assertion = {
    model.name shouldBe update.name.value
    model.description shouldBe update.description.value
    model.gateway shouldBe update.gatewayIp
  }
  
  override def withBulkCreated(quantity: Int): Resource[IO, List[Subnet[IpAddress]]] = {
    val creates = List.tabulate(quantity)(i => createStub(s"${randomName()}$i"))
    Resource.make(service.create(creates))(_.traverse_(stub => service.delete(stub.id)))
  }
  
  
}