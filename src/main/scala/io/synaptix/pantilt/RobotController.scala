package io.synaptix.pantilt

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout

object RobotController {

  def props(implicit timeout: Timeout) = Props(new RobotController)

  def name = "robot-controller"

  sealed trait MoveResponse

  case class MoveTo(pan: Double, tilt: Double)

  case class RobotConnected(tcpConnection: ActorRef)

  case class RobotDisonnected(robotActor: ActorRef)
  case object RobotMoved extends MoveResponse
  case object RobotNotConnected extends MoveResponse
}

class RobotController(implicit timeout: Timeout) extends Actor with ActorLogging {

  import RobotController._

  var robot: Option[ActorRef] = None

  override def receive = {

    case RobotConnected(connection) =>
      log.debug("Robot connected")
      val robotActor = context.actorOf(Robot.props(connection), Robot.name)
      this.robot = Some(robotActor)

    case RobotDisonnected(robotActor: ActorRef) =>
      log.debug("Robot disconnected")
      robot = None

    case msg@MoveTo(pan, tilt) =>
      log.debug(s"Move To $pan, $tilt REQUESTED")
      def noRobot() = {
        log.debug(s"No robot connected")
        sender() ! RobotNotConnected
      }
      def move(robot: ActorRef) = {
        log.debug(s"Moving robot to $pan, $tilt")
        robot.forward(msg)
        sender() ! RobotMoved
      }
      robot.fold(noRobot())(move)
  }
}
