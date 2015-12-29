package io.synaptix.pantilt

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.util.Timeout
import io.synaptix.pantilt.RobotController.RobotConnected


object RobotServer {
  def props(robotController: ActorRef)(implicit timeout: Timeout) = Props(new RobotServer(robotController))

  def name = "robot-server"
}

class RobotServer(robotController: ActorRef)(implicit timeout: Timeout) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Tcp.Bound(localAddress) =>
      log.debug(s"tcp server bound to $localAddress")

    case Tcp.CommandFailed(_: Tcp.Bind) =>
      log.debug("tcp command failed - stopping...")
      context stop self

    case Tcp.Connected(remote, local) =>
      val connection = sender()
      robotController ! RobotConnected(connection)
  }
}
