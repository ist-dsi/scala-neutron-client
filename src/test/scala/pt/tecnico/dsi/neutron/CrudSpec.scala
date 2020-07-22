package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import cats.implicits._
import io.circe.{Decoder, Encoder}
import org.scalatest.Assertion
import pt.tecnico.dsi.neutron.models.{Model, Network}
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}
import pt.tecnico.dsi.openstack.common.models.WithId

abstract class CrudSpec[T <: Model](val name: String, val service: NeutronClient[IO] => CrudService[IO, T])
  (implicit val encoder: Encoder[T#Create], val decoder: Decoder[WithId[T#Read]])
  extends Utils {

  val displayName: String = name.capitalize
  val updateStub: IO[T#Update]
  def updateComparator(read: T#Read, update: T#Update): Assertion

  val withStubCreated: Resource[IO, (WithId[T#Read], CrudService[IO, T])]
  val withNetworkCreated: Resource[IO, (WithId[Network.Read], CrudService[IO, Network])] = {
    val create = for {
      neutron <- client
      network <- neutron.networks.create {
        Network.Create()
      }
    } yield (network, neutron.networks)
    Resource.make(create) { case (network, service) => service.delete(network.id) }
  }

  s"$displayName service" should {

    "create and get" in withStubCreated.use[IO, Assertion] {
      case (stub, service) => service.get(stub.id).idempotently(_ shouldBe stub)
    }

    "delete" in withStubCreated.use[IO, Assertion] {
      case (stub, service) => for {
        _ <- service.delete(stub.id)
        lst <- service.list().compile.toList
      } yield assert(!lst.exists(_.id == stub.id))
    }

    "list" in withStubCreated.use[IO, Assertion] {
      case (stub, service) => for {
        lst <- service.list().compile.toList
      } yield assert(lst.exists(_.id == stub.id))
    }

    "update" in withStubCreated.use[IO, Assertion] {
      case (stub, service) => for {
        ustub <- updateStub
        updated <- service.update(stub.id, ustub)
      } yield updateComparator(updated, ustub)
    }
  }
}

trait BulkCreateSpec[T <: Model] {
  self: CrudSpec[T] =>

  val bulkService: NeutronClient[IO] => BulkCreate[IO, T]
  val n = 5

  val withBulkCreated: Resource[IO, List[WithId[T#Read]]]

  s"$displayName service" should {
    "create in bulk and get" in withBulkCreated.use[IO, Assertion] { createdStubs =>
      for {
        neutron <- client
        fetchedStubs <- createdStubs.traverse(t => service(neutron).get(t.id))
      } yield assert(fetchedStubs.zip(createdStubs).forall { case (a, b) => a == b })
    }
  }
}
