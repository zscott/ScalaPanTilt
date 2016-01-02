package io.synaptix.pantilt

import akka.actor.{Terminated, ActorRef, Props}
import akka.util.Timeout
import io.synaptix.FutureHelpers
import io.synaptix.pantilt.RestApi.RegisterRobotController
import io.synaptix.pantilt.RobotController.RobotMoved
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.httpx.PlayTwirlSupport._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object RestApi {
  def props(implicit timeout: Timeout) = Props(new RestApi)

  def name = "rest-api"

  case class RegisterRobotController(robotController: ActorRef)
}

class RestApi(implicit timeout: Timeout) extends HttpServiceActor
with RestRoutes {
  implicit val htmlMarshaller = twirlHtmlMarshaller // prevents IntelliJ from removing import spray.httpx.PlayTwirlSupport._

  implicit val requestTimeout = timeout

  var robotControllers: Set[ActorRef] = Set()

  def registration: Receive = {

    case RegisterRobotController(robotController) =>
      context watch robotController
      robotControllers += robotController

    case Terminated(actor) =>
      // TODO (ZS) - Doesn't handle the case where this is a local actor properly
      // TODO (ZS) - In the case where this is a local actor... restart it?
      robotControllers -= actor
  }

  def receive = registration orElse runRoute(routes)

  implicit def executionContext = context.dispatcher
}

trait RestRoutes extends HttpService with RobotControllerApi with EventMarshalling {

  def routes: Route = indexRoute ~ staticRoute ~ positionRoute

  def indexRoute = pathSingleSlash {
    get {
      complete {
        io.synaptix.pantilt.html.index.render()
      }
    }
  }

  def staticRoute =
    get {
      pathPrefix("static") {
        getFromResourceDirectory("static")
      }
    }

  def positionRoute =
    pathPrefix("position") {
      pathEndOrSingleSlash {
        post {
          // POST /position
          entity(as[PositionDescription]) { pd =>
            val responses = moveTo(pd)
            val eventualSet: Future[Set[Any]] = Future.sequence(responses)
            onComplete(eventualSet) { s =>
              s match {
                case Success(set) => {
                  if (set.contains(RobotMoved))
                    complete(StatusCodes.OK)
                  else
                    complete(StatusCodes.ServiceUnavailable)
                }
                case Failure(ex) => complete(StatusCodes.InternalServerError)
              }
            }
          }
        }
      }
    }
}

trait RobotControllerApi {

  import RobotController._
  import akka.pattern.ask

  def robotControllers: Set[ActorRef]

  implicit def executionContext: ExecutionContext

  implicit def requestTimeout: Timeout

  def moveTo(pd: PositionDescription) = {
    robotControllers.map(_ ? MoveTo(pd.pan, pd.tilt))
  }
}
