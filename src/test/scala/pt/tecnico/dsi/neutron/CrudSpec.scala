package pt.tecnico.dsi.neutron

import cats.effect.IO
import cats.implicits._
import io.circe.{Decoder, Encoder}
import org.scalatest.Assertion
import pt.tecnico.dsi.neutron.models.Model
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}
import pt.tecnico.dsi.openstack.common.models.WithId

abstract class CrudSpec[T <: Model](val name: String, val service: NeutronClient[IO] => CrudService[IO, T])
  (implicit val encoder: Encoder[T#Create], val decoder: Decoder[WithId[T#Read]])
  extends Utils {

  val displayName: String = name.capitalize
  val createStub: IO[T#Create]
  val updateStub: IO[T#Update]
  def updateComparator(read: T#Read, update: T#Update): Assertion

  val withSubCreated: IO[(WithId[T#Read], CrudService[IO, T])] =
    for {
      neutron <- client
      stub <- createStub
      crudService = service(neutron)
      createdStub <- crudService.create(stub)
    } yield (createdStub, crudService)


  s"$displayName service" should {

    "create and get" in withSubCreated .flatMap { case (stub, service) =>
      service.get(stub.id) .idempotently(_ shouldBe stub)
    }

    "delete" in {
      for {
        (stub, service) <- withSubCreated
        _ <- service.delete(stub.id)
        lst <- service.list().compile.toList
      } yield assert(!lst.exists(_.id == stub.id))
    }

    "list" in {
      for {
        (stub, service) <- withSubCreated
        lst <- service.list().compile.toList
      } yield assert(lst.exists(_.id == stub.id))
    }

    "update" in {
      for {
        ustub <- updateStub
        (stub, service) <- withSubCreated
        updated <- service.update(stub.id, ustub)
      } yield updateComparator(updated, ustub)
    }
  }
}

trait BulkCreateSpec[T <: Model] {
  self: CrudSpec[T] =>

  val bulkService: NeutronClient[IO] => BulkCreate[IO, T]
  val n = 5

  s"$displayName service" should {
    "create in bulk" in {
      for {
        neutron <- client
        stubs <- List.fill(n)(createStub).sequence
        createdStubs <- bulkService(neutron).create(stubs)
        fetchedStubs <- createdStubs.traverse(t => service(neutron).get(t.id))
      } yield assert(fetchedStubs.zip(createdStubs).forall { case (a, b) => a == b })
    }
  }
}
