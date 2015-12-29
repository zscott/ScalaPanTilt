package io.synaptix.pantilt

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.io.Tcp.PeerClosed
import akka.util.Timeout
import io.synaptix.pantilt.RobotController.{MoveTo, RobotDisonnected}

object Robot {
  def props(connection: ActorRef)(implicit timeout: Timeout) = Props(new Robot(connection))

  def name = "robot"
}

class Robot(connection: ActorRef)(implicit timeout: Timeout) extends Actor with ActorLogging {

  connection ! Tcp.Register(self)

  override def receive: Receive = {

    case Tcp.Received(data) =>
      log.debug(s"received data from robot: $data")

    case PeerClosed =>
      log.debug(s"lost connection to robot")
      context.parent ! RobotDisonnected(self)
      context stop self

    case MoveTo(pan: Double, tilt: Double) =>

  }
}
