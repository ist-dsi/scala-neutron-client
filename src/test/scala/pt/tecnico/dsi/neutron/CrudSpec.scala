package pt.tecnico.dsi.neutron

import cats.effect.{IO, Resource}
import cats.implicits._
import io.circe.{Decoder, Encoder}
import org.scalatest.Assertion
import pt.tecnico.dsi.neutron.models.{Model, Network}
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}
import pt.tecnico.dsi.openstack.common.models.WithId

abstract class CrudSpec[T <: Model](val name: String)
  (implicit val encoder: Encoder[T#Create], val decoder: Decoder[WithId[T#Read]])
  extends Utils {

  abstract val service: CrudService[IO, T]

  val displayName: String = name.capitalize
  val updateStub: IO[T#Update]
  def updateComparator(read: T#Read, update: T#Update): Assertion

  val withStubCreated: Resource[IO, WithId[T#Read]]

  val withNetworkCreated: Resource[IO, WithId[Network.Read]] = {
    val created = client.networks.create { Network.Create() }
    Resource.make(created)(x => client.networks.delete(x.id))
  }

  s"$displayName service" should {

    "create and get" in withStubCreated.use[IO, Assertion] {
      stub => service.get(stub.id).idempotently(_ shouldBe stub)
    }

    "delete" in withStubCreated.use[IO, Assertion] { stub =>
      for {
        _ <- service.delete(stub.id)
        lst <- service.list().compile.toList
      } yield assert(!lst.exists(_.id == stub.id))
    }

    "list" in withStubCreated.use[IO, Assertion] { stub =>
      for {
        lst <- service.list().compile.toList
      } yield assert(lst.exists(_.id == stub.id))
    }

    "update" in withStubCreated.use[IO, Assertion] { stub =>
      for {
        ustub <- updateStub
        updated <- service.update(stub.id, ustub)
      } yield updateComparator(updated, ustub)
    }
  }
}

trait BulkCreateSpec[T <: Model] {
  self: CrudSpec[T] =>

  val service: BulkCreate[IO, T]
  val n = 5

  s"$displayName service" should {
    "create in bulk and get" in withBulkCreated.use[IO, Assertion] { createdStubs =>
      for {
        neutron <- client
        fetchedStubs <- createdStubs.traverse(t => service(neutron).get(t.id))
      } yield assert(fetchedStubs.zip(createdStubs).forall { case (a, b) => a == b })
    }
  }
}
