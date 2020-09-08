package pt.tecnico.dsi.openstack.neutron

import cats.effect.{IO, Resource}
import cats.implicits._
import io.circe.{Decoder, Encoder}
import org.scalatest.Assertion
import org.scalatest.OptionValues._
import pt.tecnico.dsi.openstack.neutron.models.{Model, Network}
import pt.tecnico.dsi.openstack.neutron.services.{BulkCreate, CrudService}

abstract class CrudSpec[T <: Model](val name: String)
  (implicit val encoder: Encoder[T#Create], val decoder: Decoder[T#Read])
  extends Utils {

  val service: CrudService[IO, T]

  val displayName: String = name.capitalize
  val updateStub: IO[T#Update]
  def updateComparator(read: T#Read, update: T#Update): Assertion

  val withStubCreated: Resource[IO, T#Read]

  val withNetworkCreated: Resource[IO, Network.Read] = {
    val created = neutron.networks.create { Network.Create() }
    Resource.make(created)(stub => neutron.networks.delete(stub.id))
  }

  s"$displayName service" should {

    "create and get" in withStubCreated.use[IO, Assertion] {
      stub => service.get(stub.id).idempotently(_.value shouldBe stub)
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
  def withBulkCreated(quantity: Int = 5): Resource[IO, List[T#Read]]

  s"$displayName service" should {
    "create in bulk and get" in withBulkCreated().use[IO, Assertion] { createdStubs =>
      for {
        fetchedStubs <- createdStubs.traverse(stub => service.get(stub.id))
      } yield assert(fetchedStubs.zip(createdStubs).forall { case (a, b) => a.value == b })
    }
  }
}
