package io.synaptix.pantilt

import akka.actor.ActorRef
import akka.util.Timeout
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing._

import scala.concurrent.ExecutionContext

class RestApi(timeout: Timeout) extends HttpServiceActor
with RestRoutes {
  implicit val requestTimeout = timeout

  def receive = runRoute(routes)

  implicit def executionContext = context.dispatcher

  def createRobotController() = context.actorOf(RobotController.props, RobotController.name)
}

trait RestRoutes extends HttpService with RobotControllerApi with EventMarshalling {

  import StatusCodes._

  def routes: Route = positionRoute // ~ otherRoute ~ yetAnotherRoute

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
            }
          }
        }
      }
    }
}

trait RobotControllerApi {

  import RobotController._
  import akka.pattern.ask

  lazy val robotController = createRobotController()

  def createRobotController(): ActorRef

  implicit def executionContext: ExecutionContext

  implicit def requestTimeout: Timeout

  def moveTo(pd: PositionDescription) = {
    robotController.ask(MoveTo(pd.pan, pd.tilt))
  }
}