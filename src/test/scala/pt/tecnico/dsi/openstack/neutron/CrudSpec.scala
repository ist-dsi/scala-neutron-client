package pt.tecnico.dsi.openstack.neutron

import scala.util.Try
import cats.effect.{IO, Resource}
import cats.implicits._
import org.http4s.Query
import org.http4s.client.UnexpectedStatus
import org.scalatest.Assertion
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.neutron.services.BulkCreate

abstract class CrudSpec[Model <: Identifiable, Create, Update](val name: String) extends Utils {
  def service: CrudService[IO, Model, Create, Update]
  
  def createStub(name: String): Create
  def compareCreate(create: Create, model: Model): Assertion
  
  val updateStub: Update
  def compareUpdate(update: Update, model: Model): Assertion
  
  def compareGet(get: Model, model: Model): Assertion = get shouldBe model
  
  lazy val resource: Resource[IO, Model] = resourceCreator(service)(createStub)
  
  s"The ${name}s service" should {
    s"list ${name}s" in resource.use[IO, Assertion] { model =>
      service.list().compile.toList.idempotently { models =>
        models.exists(m => Try(compareGet(m, model)).isSuccess) shouldBe true
      }
    }
    
    s"create ${name}s" in {
      val name = randomName()
      val stub = createStub(name)
      val repetitions = 3
      for {
        _ <- service.create(stub).idempotently(compareCreate(stub, _), repetitions)
        list <- service.list(Query.fromPairs("name" -> name, "project_id" -> project.id, "limit" -> repetitions.toString)).compile.toList
        _ <- list.parTraverse_(service.delete(_))
      } yield list.size shouldBe 1
    }
    
    s"get ${name}s (existing id)" in resource.use[IO, Assertion] { model =>
      service.get(model.id).idempotently(m => compareGet(m.value, model))
    }
    s"get ${name}s (non-existing id)" in {
      service.get("non-existing-id").idempotently(_ shouldBe None)
    }
    
    s"apply ${name}s (existing id)" in resource.use[IO, Assertion] { model =>
      service.apply(model.id).idempotently(m => compareGet(m, model))
    }
    s"apply ${name}s (non-existing id)" in {
      service.apply("non-existing-id").attempt.idempotently(_.left.value shouldBe a [UnexpectedStatus])
    }
    
    s"update ${name}s" in resource.use[IO, Assertion] { model =>
      service.update(model.id, updateStub).idempotently(compareUpdate(updateStub, _))
    }
    
    s"delete a $name" in resource.use[IO, Assertion] { model =>
      service.delete(model.id).idempotently(_ shouldBe ())
    }
  }
}

trait BulkCreateSpec[Model <: Identifiable, Create] { self: CrudSpec[Model, Create, _] =>
  val service: CrudService[IO, Model, Create, _] with BulkCreate[IO, Model, Create]
  def withBulkCreated(quantity: Int = 5): Resource[IO, List[Model]]

  s"The ${name}s service" should {
    "create in bulk and get" in withBulkCreated().use[IO, Assertion] { createdStubs =>
      for {
        fetchedStubs <- createdStubs.traverse(stub => service.get(stub.id))
      } yield assert(fetchedStubs.zip(createdStubs).forall { case (a, b) => a.value == b })
    }
  }
}
