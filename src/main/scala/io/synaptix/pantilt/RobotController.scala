package io.synaptix.pantilt

import akka.actor.{Actor, Props}
import akka.util.Timeout

object RobotController {

  def props(implicit timeout: Timeout) = Props(new RobotController)

  def name = "robot-controller"

  sealed trait MoveResponse

  case class MoveTo(pan: Double, tilt: Double)

  case object RobotMoved extends MoveResponse

  case object RobotNotConnected extends MoveResponse

}

class RobotController(implicit timeout: Timeout) extends Actor {

  import RobotController._

  val robotChannel: Option[RobotChannel] = None

  override def receive = {
    case MoveTo(pan, tilt) =>
      def noRobot() = sender() ! RobotNotConnected
      def move(robotChannel: RobotChannel) = {
        robotChannel.updatePosition(pan, tilt)
        sender() ! RobotMoved
      }
      robotChannel.fold(noRobot())(move)
  }
}
