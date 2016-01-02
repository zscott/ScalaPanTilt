package io.synaptix.pantilt

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.util.Timeout
import io.synaptix.pantilt.RobotController.RequestDropped
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.httpx.PlayTwirlSupport._

import scala.concurrent.ExecutionContext

object RestApi {
  def props(robotController: ActorRef)(implicit timeout: Timeout) = Props(new RestApi(robotController))

  def name = "rest-api"
}

class RestApi(val robotController: ActorRef)(implicit timeout: Timeout) extends HttpServiceActor
with RestRoutes {
  implicit val requestTimeout = timeout

  def receive = runRoute(routes)

  implicit def executionContext = context.dispatcher
}

trait RestRoutes extends HttpService with RobotControllerApi with EventMarshalling {

  import StatusCodes._

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
            onSuccess(moveTo(pd)) {
              case RobotController.RobotMoved => complete(OK)
              case RobotController.RobotNotConnected =>
                val err = Error(s"No Robot Connected")
                complete(NotFound, err)
              case RequestDropped(reason) =>
                complete(TooManyRequests, s"Command not sent to robot: $reason")
            }
          }
        }
      }
    }
}

trait RobotControllerApi {

  import RobotController._
  import akka.pattern.ask

  def robotController: ActorRef

  implicit def executionContext: ExecutionContext

  implicit def requestTimeout: Timeout

  def moveTo(pd: PositionDescription) = {
    robotController.ask(MoveTo(pd.pan, pd.tilt))
  }
}
