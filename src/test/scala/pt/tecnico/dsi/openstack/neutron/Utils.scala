package pt.tecnico.dsi.openstack.neutron

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random
import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.instances.list._
import cats.syntax.traverse._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import org.http4s.{Headers, Uri}
import org.log4s._
import org.scalatest._
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.typelevel.ci.CIString
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project

abstract class Utils extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
  val logger: Logger = getLogger

  implicit override def executionContext: ExecutionContextExecutor = ExecutionContext.global

  implicit val timer: Timer[IO] = IO.timer(executionContext)
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)

  val (_httpClient, finalizer) = BlazeClientBuilder[IO](global)
    .withResponseHeaderTimeout(20.seconds)
    .withCheckEndpointAuthentication(false)
    .resource.allocated.unsafeRunSync()

  implicit val httpClient: Client[IO] = Logger(
    logHeaders = true,
    logBody = true,
    redactHeadersWhen = (Headers.SensitiveHeaders ++ List(CIString("X-Auth-Token"), CIString("X-Subject-Token"))).contains)(_httpClient)

  val keystone: KeystoneClient[IO] = KeystoneClient.fromEnvironment().unsafeRunSync()
  val neutron: NeutronClient[IO] = {
    val neutronUrl = keystone.session.urlOf("network", sys.env("OS_REGION_NAME"))
                             .getOrElse(throw new Exception("Could not find \"network\" service in the catalog"))
    new NeutronClient[IO](Uri.unsafeFromString(neutronUrl), keystone.authToken)
  }
  
  // Not very purely functional :(
  val random = new Random()
  def randomName(): String = random.alphanumeric.take(10).mkString.dropWhile(_.isDigit).toLowerCase
  def withRandomName[T](f: String => IO[T]): IO[T] = IO.delay(randomName()).flatMap(f)
  
  def resourceCreator[R <: Identifiable, Create](service: CrudService[IO, R, Create, _])(create: String => Create): Resource[IO, R] = {
    Resource.make(withRandomName(name => service(create(name))))(model => service.delete(model.id))
  }
  
  // This way we use the same project for every test, and make the logs smaller and easier to debug.
  // Although this is not very purely functional :(
  val (project, deleteProject) = resourceCreator(keystone.projects)(Project.Create(_)).allocated.unsafeRunSync()
  override protected def afterAll(): Unit = {
    deleteProject.unsafeRunSync()
    finalizer.unsafeRunSync()
  }
  
  //val withStubNetwork: Resource[IO, Network] = resourceCreator(neutron.networks)(Network.Create(_))
  
  implicit class RichIO[T](io: IO[T]) {
    def idempotently(test: T => Assertion, repetitions: Int = 3): IO[Assertion] = {
      require(repetitions >= 2, "To test for idempotency at least 2 repetitions must be made")
      io.flatMap { firstResult =>
        // If this fails we do not want to mask its exception with "Operation is not idempotent".
        // Because failing in the first attempt means whatever is being tested in `test` is not implemented correctly.
        test(firstResult)
        (2 to repetitions).toList.traverse { _ =>
          io
        } map { results =>
          // And now we want to catch the exception because if `test` fails here it means it is not idempotent.
          try {
            results.foreach(test)
            succeed
          } catch {
            case e: TestFailedException =>
              val numberOfDigits = Math.floor(Math.log10(repetitions.toDouble)).toInt + 1
              val resultsString = (firstResult +: results).zipWithIndex
                                                          .map { case (result, i) =>
                                                            s" %${numberOfDigits}d: %s".format(i + 1, result)
                                                          }.mkString("\n")
              throw e.modifyMessage(_.map(message =>
                s"""Operation is not idempotent. Results:
                   |$resultsString
                   |$message""".stripMargin))
          }
        }
      }
    }
  }
  
  import scala.language.implicitConversions
  implicit def io2Future(io: IO[Assertion]): Future[Assertion] = io.unsafeToFuture()
}
