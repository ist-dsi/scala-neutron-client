package pt.tecnico.dsi.openstack.neutron

import scala.util.Random
import cats.effect.IO
import com.comcast.ip4s.Ipv4Address
import org.scalatest.Assertion
import org.scalatest.OptionValues.*
import pt.tecnico.dsi.openstack.neutron.models.{Network, Router, Subnet}
import pt.tecnico.dsi.openstack.neutron.services.Routers

final class RoutersSpec extends CrudSpec[Router, Router.Create, Router.Update]("router"):
  override val service: Routers[IO] = neutron.routers
  
  override def createStub(name: String): Router.Create = Router.Create(
    name,
    "a description",
    projectId = Some(project.id),
  )
  override def compareCreate(create: Router.Create, model: Router): Assertion =
    model.name shouldBe create.name
    model.projectId shouldBe create.projectId.value
    model.description shouldBe create.description
  
  override val updateStub: Router.Update = Router.Update(
    name = Some(randomName()),
    description = Some("a better and improved description"),
  )
  override def compareUpdate(update: Router.Update, model: Router): Assertion =
    model.name shouldBe update.name.value
    model.description shouldBe update.description.value
  
  override def compareGet(get: Router, model: Router): Assertion =
    // After a router create openstack performs extra operations on the router and updates it (great move right there </sarcasm>)
    // causing the revision and updatedAt to change.
    // The availabilityZones will be set by openstack (using the availabilityZoneHints) so its value wont be the same
    get.id shouldBe model.id
    get.projectId shouldBe model.projectId
    get.name shouldBe model.name
    get.description shouldBe model.description
    get.adminStateUp shouldBe model.adminStateUp
    get.status shouldBe model.status
    get.externalGatewayInfo.zip(model.externalGatewayInfo).map { case (getInfo, modelInfo) =>
      getInfo.networkId shouldBe modelInfo.networkId
      getInfo.enableSnat shouldBe modelInfo.enableSnat
      getInfo.externalFixedIps should contain (modelInfo.externalFixedIps)
    }
    // get.revision shouldBe model.revision + 2 // This works but its wrong to implement it
    get.routes shouldBe model.routes
    get.distributed shouldBe model.distributed
    get.ha shouldBe model.ha
    get.availabilityZoneHints shouldBe model.availabilityZoneHints
    //get.availabilityZones shouldBe model.availabilityZones
    get.flavorId shouldBe model.flavorId
    get.conntrackHelpers shouldBe model.conntrackHelpers
    get.createdAt shouldBe model.createdAt
    //get.updatedAt shouldBe model.updatedAt
    get.tags shouldBe model.tags
  
  // These are failing because the VM to which we are testing against has incorrect permissions set and we receive:
  // The request you have made requires authentication
  s"The ${name}s service" should {
    val resources = for
      router <- resource
      network <- resourceCreator(neutron.networks)(name => Network.Create(
        name = name,
        projectId = Some(project.id),
      ))
      subnet <- resourceCreator(neutron.subnets)(name => Subnet.Create(
        name,
        network.id,
        "a description",
        Ipv4Address.fromString(s"192.168.${Random.between(50, 250)}.0").map(_ / 24),
        projectId = Some(project.id)
      ))
    yield (router, network, subnet)

    "addInterface" in resources.use { case (router, network, subnet) =>
      for
        first <- service.on(router).addInterface(subnet)
        second <- service.on(router).addInterface(subnet)
        // We must remove the subnet otherwise the deletes will fail
        _ <- service.on(router).removeInterface(subnet)
      yield
        first.value.routerId shouldBe router.id
        first.value.networkId shouldBe network.id
        first.value.projectId shouldBe project.id
        first.value.subnetId shouldBe subnet.id
        second shouldBe None
    }
    "removeInterface" in resources.use { case (router, _, subnet) =>
      for
        _ <- service.on(router).addInterface(subnet)
        result <- service.on(router).removeInterface(subnet).idempotently(_ shouldBe())
      yield result
    }
  }