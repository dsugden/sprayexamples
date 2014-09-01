package examples

import akka.actor.{ ActorPath, Props, ActorSystem }
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest._
import spray.http.HttpHeaders.RawHeader
import spray.testkit.ScalatestRouteTest

import scala.concurrent.duration._

object AcceptHeaderVersioningSpec {
  import spray.util.Utils
  val testSystem = ActorSystem("foo-spray-spec")
}



class AcceptHeaderVersioningSpec extends FeatureSpecLike
with GivenWhenThen
with ScalatestRouteTest
with MustMatchers
with BeforeAndAfterAll
with FooRoute {

  import  AcceptHeaderVersioningSpec._

  implicit val timeout = Timeout(5 seconds)

  override protected def createActorSystem(): ActorSystem = testSystem

  def actorRefFactory = testSystem
  implicit val ec = system.dispatcher

  override protected def afterAll() {
    system.shutdown()
    system.awaitTermination(10.seconds)
  }

  val fooActor = system.actorOf(Props[FooActor], "foo-actor")
  implicit val fooActorPath: ActorPath = fooActor.path



  feature("FooProtocol versions") {
    scenario("A HelloFoo json payload is posted to the foo route with v1 Accept header") {
      import FooProtocol._

      Given("the route is accessed")
      import Json4sProtocol._
      Post("/foo", HelloFoo("Dave")) ~> addHeader(RawHeader("Accept", "application/vnd.ww.v1.foo+json")) ~> fooRoute ~> check {
        When("the route is handled")
        if (!handled) fail("Request not handled.")
        Then("the response is FooResponse")
        responseAs[FooResponse] must be(FooResponse("Hello back Dave"))
      }
    }
    scenario("A HelloFoo json payload is posted to the foo route with v2 Accept header") {
      import FooProtocolV2._

      Given("the route is accessed")
      import Json4sProtocol._
      Post("/foo", HelloFoo("Dave","baboo")) ~> addHeader(RawHeader("Accept", "application/vnd.ww.v2.foo+json")) ~> fooRoute ~> check {
        When("the route is handled")
        if (!handled) fail("Request not handled.")
        Then("the response is FooResponse")
        responseAs[FooResponse] must be(FooResponse("Hello back Dave baboo"))
      }
    }
  }

}
