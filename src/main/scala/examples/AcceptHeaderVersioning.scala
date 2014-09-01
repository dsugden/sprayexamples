package examples


import akka.actor.{Actor, ActorLogging, ActorPath}
import akka.pattern.AskSupport
import akka.util.Timeout
import examples.VersionMediaTypes._
import org.json4s.{DefaultFormats, Formats}
import spray.http.ContentType._
import spray.http.{HttpEntity, MediaType, MediaTypes}
import spray.httpx.Json4sSupport
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller}
import spray.httpx.unmarshalling.Unmarshaller
import spray.routing.Directive._
import spray.routing._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}


object Json4sProtocol extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats
}


class FooActor extends Actor with ActorLogging {
  def receive: PartialFunction[Any, Unit] = {
    // V1
    case msg @ FooProtocol.HelloFoo(hi) =>
      sender ! FooProtocol.FooResponse(s"Hello back $hi")
    // V2
    case msg @ FooProtocolV2.HelloFoo(hi,there) =>
      sender ! FooProtocolV2.FooResponse(s"Hello back $hi $there" )
  }
}


/**
 * Foo Version Media Types
 */
object VersionMediaTypes{
  lazy val `application/vnd.ww.v2.foo+json`  =
    MediaTypes.register(MediaType.custom("application/vnd.ww.v2.foo+json"))
  lazy val `application/vnd.ww.v1.foo+json`  =
    MediaTypes.register(MediaType.custom("application/vnd.ww.v1.foo+json"))
}




/**
 * FooProtocol v1
 */
object FooProtocol {
  sealed trait FooActorMessage
  case class HelloFoo(hi: String) extends FooActorMessage
  case class FooResponse(helloBackTo: String) extends FooActorMessage

  import examples.Json4sProtocol._
  import org.json4s.native.Serialization.{read, write => swrite}

  implicit val FooUnmarshaller: Unmarshaller[HelloFoo] =
    Unmarshaller[HelloFoo](`application/vnd.ww.v1.foo+json`) {
      case HttpEntity.NonEmpty(contentType, data) =>
        read[HelloFoo](data.asString)
    }

  implicit val FooMarshaller: Marshaller[HelloFoo] =
    Marshaller.of[HelloFoo](`application/vnd.ww.v1.foo+json`) { (value, contentType, ctx) =>
      ctx.marshalTo(HttpEntity(contentType, swrite(value)))
    }
  implicit val HelloBackUnmarshaller: Unmarshaller[FooResponse] =
    Unmarshaller[FooResponse](`application/vnd.ww.v1.foo+json`) {
      case HttpEntity.NonEmpty(contentType, data) =>
        read[FooResponse](data.asString)
    }

  implicit val HelloBackMarshaller: Marshaller[FooResponse] =
    Marshaller.of[FooResponse](`application/vnd.ww.v1.foo+json`) { (value, contentType, ctx) =>
      ctx.marshalTo(HttpEntity(contentType, swrite(value)))
    }
}


/**
 * FooProtocol v2
 */
object FooProtocolV2 {
  sealed trait FooActorMessage
  case class HelloFoo(hi: String, there:String) extends FooActorMessage
  case class FooResponse(helloBackTo: String) extends FooActorMessage


  import examples.Json4sProtocol._
  import org.json4s.native.Serialization.{read, write => swrite}


  implicit val V2HelloUnmarshaller:Unmarshaller[FooProtocolV2.HelloFoo] =
    Unmarshaller[FooProtocolV2.HelloFoo](`application/vnd.ww.v2.foo+json`) {
      case HttpEntity.NonEmpty(contentType, data) =>
        read[HelloFoo](data.asString)
    }

  implicit val V2HelloMarshaller:Marshaller[FooProtocolV2.HelloFoo] =
    Marshaller.of[HelloFoo](`application/vnd.ww.v2.foo+json`) { (value, contentType, ctx) =>
      ctx.marshalTo(HttpEntity(contentType, swrite(value)))
    }
  implicit val V2HelloBackUnmarshaller:Unmarshaller[FooProtocolV2.FooResponse] =
    Unmarshaller[FooResponse](`application/vnd.ww.v2.foo+json`) {
      case HttpEntity.NonEmpty(contentType, data) =>
        read[FooResponse](data.asString)
    }

  implicit val V2HelloBackMarshaller:Marshaller[FooProtocolV2.FooResponse] =
    Marshaller.of[FooResponse](`application/vnd.ww.v2.foo+json`) { (value, contentType, ctx) =>
      ctx.marshalTo(HttpEntity(contentType, swrite(value)))
    }
}




/**
 * Foo restful interface.
 */
trait FooRoute extends HttpService with AskSupport {

  import examples.FooProtocol._
  import examples.FooProtocolV2._

  implicit val timeout: Timeout
  implicit val fooActorPath: ActorPath
  implicit val ec: ExecutionContext

  /** This route supports foo v1 and foo v2 */
  val fooRoute =
    post {
      path("foo") {

        implicit ctx =>

          lazy val actorSelection = actorRefFactory.actorSelection(fooActorPath)

          def actComplete[T](implicit marshaller: ToResponseMarshaller[T]):Try[T] => Route =  {
            case Success(v) => complete(v)
            case Failure(e) => complete(e)
          }

          /**
           * Here the version is determined as part of the Unmarshalling, as
           * per the Accept(MediaType)
           *
           * The actor that handles the request also matches on the different
           * version ( FooProtocol | FooProtocolV2 )
           *
           */

          // V1 of Hello
          (entity(as[FooProtocol.HelloFoo]) { cmd =>
            onComplete(( actorSelection ? cmd).mapTo[FooProtocol.FooResponse]){
              actComplete[FooProtocol.FooResponse]
            }
          } ~
            // V2 of Hello
            entity(as[FooProtocolV2.HelloFoo]) { cmd =>
              onComplete(( actorSelection ? cmd).mapTo[FooProtocolV2.FooResponse]) {
                actComplete[FooProtocolV2.FooResponse]
              }
            })(ctx)
      }
    }
}
// format: ON
