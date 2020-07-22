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

  val service: CrudService[IO, T]

  val displayName: String = name.capitalize
  val updateStub: IO[T#Update]
  def updateComparator(read: T#Read, update: T#Update): Assertion

  val withStubCreated: Resource[IO, WithId[T#Read]]

  val withNetworkCreated: Resource[IO, WithId[Network.Read]] = {
    val created = client.networks.create { Network.Create() }
    Resource.make(created)(stub => client.networks.delete(stub.id))
  }

  s"$displayName service" should {

    "create and get" in withStubCreated.use[IO, Assertion] {
      stub => service.get(stub.id).idempotently(_ shouldBe stub)
    }

    "delete" in withStubCreated.use[IO, Assertion] { stub =>
      for {
        _ <- service.delete(stub.id).idempotently(_ shouldBe ())
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
        isIdempotent <- service.update(stub.id, ustub).idempotently(x => updateComparator(x, ustub))
      } yield isIdempotent
    }
  }
}

trait BulkCreateSpec[T <: Model] {
  self: CrudSpec[T] =>

  val service: CrudService[IO, T] with BulkCreate[IO, T]
  def withBulkCreated(quantity: Int = 5): Resource[IO, List[WithId[T#Read]]]

  s"$displayName service" should {
    "create in bulk and get" in withBulkCreated().use[IO, Assertion] { createdStubs =>
      for {
        fetchedStubs <- createdStubs.traverse(stub => service.get(stub.id))
      } yield assert(fetchedStubs.zip(createdStubs).forall { case (a, b) => a == b })
    }
  }
}
