package io.synaptix.pantilt

import akka.actor._
import akka.util.Timeout
import io.synaptix.ServiceException

object RobotController {

  def props(implicit timeout: Timeout) = Props(new RobotController)

  def name = "robot-controller"

  case class MoveTo(pan: Double, tilt: Double)

  case class RobotConnected(tcpConnection: ActorRef)
  case class RobotDisconnected(robotActor: ActorRef)


  sealed trait MoveResponse
  case object RobotMoved extends MoveResponse
  case class RequestDropped(reason: String) extends MoveResponse
  case object RobotNotConnected extends MoveResponse
}

class RobotController(implicit timeout: Timeout) extends Actor with ActorLogging {

  import RobotController._

  var robotConnection: Option[ActorRef] = None

  override def receive = {

    case RobotConnected(connection) =>
      log.info("Robot connected")
      val robotConnection = context.actorOf(RobotTcpConnection.props(connection), RobotTcpConnection.name)
      this.robotConnection = Some(robotConnection)
      context watch robotConnection

    case Terminated(terminatedActorRef) =>
      def terminated(currentRobotConnection: ActorRef) = {
        if (currentRobotConnection == terminatedActorRef) {
          log.info("RobotTcpConnection Terminated")
          robotConnection = None
        }
      }
      robotConnection.foreach(terminated)

    case RobotDisconnected(robotActor: ActorRef) =>
      log.info("Robot disconnected")
      robotConnection = None

    case msg@MoveTo(pan, tilt) =>
      log.debug(s"Move To $pan, $tilt REQUESTED")
      def noRobot() = {
        log.debug(s"No robot connected")
        sender() ! RobotNotConnected
      }
      def move(robot: ActorRef) = {
        log.debug(s"Moving robot to $pan, $tilt")
        robot.forward(msg)
      }
      robotConnection.fold(noRobot())(move)
  }
}
