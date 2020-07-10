package pt.tecnico.dsi.neutron

import cats.effect.IO
import cats.implicits._
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.neutron.models.Model
import pt.tecnico.dsi.neutron.services.{BulkCreate, CrudService}
import pt.tecnico.dsi.openstack.common.models.WithId

abstract class CrudSpec[T <: Model](val name: String, val service: NeutronClient[IO] => CrudService[IO, T])
  (implicit val encoder: Encoder[T#Create], val decoder: Decoder[WithId[T#Read]])
  extends Utils {

  val displayName: String = name.capitalize
  val createStub: IO[T#Create]

  val withSubCreated: IO[(WithId[T#Read], CrudService[IO, T])] =
    for {
      neutron <- client
      stub <- createStub
      crudService = service(neutron)
      createdStub <- crudService.create(stub)
    } yield (createdStub, crudService)


  s"$displayName service" should {

    "create and get" in {
      for {
        (stub, service) <- withSubCreated
        actual <- service.get(stub.id)
      } yield actual shouldBe stub
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
      } yield assert(fetchedStubs.zip(createdStubs).forall(a => a._1 == a._2))
    }
  }
}
