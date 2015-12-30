package io.synaptix.pantilt

import akka.actor._
import akka.io.Tcp
import akka.io.Tcp.Event
import akka.util.{ByteString, Timeout}
import io.synaptix.pantilt.RobotController.{RequestDropped, RobotMoved, MoveTo, RobotDisonnected}
import scala.concurrent.duration._
import scala.language.postfixOps

object Robot {
  def props(connection: ActorRef)(implicit timeout: Timeout) = Props(new Robot(connection))

  def name = "robot"
}

class Robot(connection: ActorRef)(implicit timeout: Timeout) extends Actor with ActorLogging {

  connection ! Tcp.Register(self)

  // death pact : this actor terminates when connection breaks
  context watch connection

  implicit val ec = context.system.dispatcher

  case class Ack(requestingActor: ActorRef, cancellableAckTimer: Cancellable) extends Event
  case object AckTimeout

  def receiveErrors: Receive = {
    case Tcp.ErrorClosed(error) =>
      log.info(s"lost connection to robot: $error")
      context.parent ! RobotDisonnected(self)
      context stop self

    case _: Tcp.ConnectionClosed =>
      log.info(s"lost connection to robot")
      context.parent ! RobotDisonnected(self)
      context stop self
  }

  def receiveInReadyState: Receive = {
    case Tcp.Received(data) =>
      log.debug(s"received data from robot: $data")

    case MoveTo(pan: Double, tilt: Double) =>
      log.debug(s"moving to $pan, $tilt")
      val panByte = pan.toInt.toByte
      val tiltByte = tilt.toInt.toByte
      val data = ByteString(panByte, tiltByte)
      val cancellableAckTimer = context.system.scheduler.scheduleOnce(1 second, self, AckTimeout)
      connection ! Tcp.Write(data, Ack(sender(), cancellableAckTimer))
      context.become(receiveErrors orElse receiveInAwaitingAckState orElse unhandled, discardOld = false)
  }

  def receiveInAwaitingAckState: Receive = {
    case Ack(requestingActor, cancellableAckTimer) =>
      cancellableAckTimer.cancel()
      requestingActor ! RobotMoved
      context.unbecome()

    case AckTimeout =>
      log.error(s"Timeout waiting for Ack from robot - Assuming we are disconnected")
      context.parent ! RobotDisonnected(self)
      context stop self

    case MoveTo(pan, tilt) =>
      log.warning(s"Dropped request to move to $pan, $tilt - Waiting for ACK from robot!")
      sender() ! RequestDropped("robot temporarily busy")
  }

  def unhandled: Receive = {
    case m =>
      log.warning(s"Unhandled message: $m")
  }
  override def receive: Receive = receiveErrors orElse receiveInReadyState orElse unhandled
}
