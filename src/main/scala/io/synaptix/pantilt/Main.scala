package io.synaptix.pantilt

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem}
import akka.io.{IO, Tcp}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import io.synaptix.pantilt.RobotController.MoveTo
import spray.can.Http

import scala.util.Random

object Main extends App with RequestTimeout with HttpBinding with TcpBinding {

  val config = ConfigFactory.load()
  val restHost = config.getString("rest.host")
  val restPort = config.getInt("rest.port")
  val robotHost = config.getString("robot.host")
  val robotPort = config.getInt("robot.port")

  implicit val system = ActorSystem("pan-tilt")
  implicit val executionContext = system.dispatcher
  implicit val timeout = requestTimeout(config)

  val robotController = system.actorOf(RobotController.props, RobotController.name)

  val restApi = system.actorOf(RestApi.props(robotController), RestApi.name)
  bindHttpListener(restApi, restHost, restPort)

  val robotServer = system.actorOf(RobotServer.props(robotController), RobotServer.name)
  bindTcpServer(robotServer, robotHost, robotPort)

  while (true) {
    val pan = Random.nextInt(100)
    val tilt = Random.nextInt(100)
    robotController ! MoveTo(pan, tilt)
    Thread.sleep(Random.nextInt(1000) + 1000)
  }
}

trait RequestTimeout {

  import scala.concurrent.duration._

  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("spray.can.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}

trait HttpBinding {

  import scala.concurrent.{ExecutionContext, Future}

  implicit def system: ActorSystem

  implicit def timeout: Timeout

  def bindHttpListener(listener: ActorRef, interface: String, port: Int)(implicit ec: ExecutionContext): Future[Any] = {
    def shutdownIfNotBound(f: Future[Any])
                          (implicit system: ActorSystem, ec: ExecutionContext) = {
      f.mapTo[Http.Event].map {
        case Http.Bound(address) =>
          println(s"HTTP listener [${listener.path}] bound to $address")
        case Http.CommandFailed(cmd) =>
          println(s"HTTP listener [${listener.path}] could not bind: ${cmd.failureMessage}, shutting down.")
          system.terminate()
      }.recover {
        case e: Throwable =>
          println(s"Unexpected error binding [${listener.path}] to HTTP: ${e.getMessage}, shutting down.")
          system.terminate()
      }
    }
    val response = IO(Http).ask(Http.Bind(listener = listener, interface = interface, port = port))
    shutdownIfNotBound(response)
  }
}

trait TcpBinding {

  import scala.concurrent.{ExecutionContext, Future}

  implicit def system: ActorSystem

  implicit def timeout: Timeout

  def bindTcpServer(handler: ActorRef, interface: String, port: Int)(implicit ec: ExecutionContext): Future[Any] = {

    def shutdownIfNotBound(f: Future[Any])
                          (implicit system: ActorSystem, ec: ExecutionContext) = {
      f.mapTo[Http.Event].map {
        case Http.Bound(address) =>
          println(s"TCP handler [${handler.path}] bound to $address")
        case Http.CommandFailed(cmd) =>
          println(s"TCP handler [${handler.path}] could not bind: ${cmd.failureMessage}, shutting down.")
          system.terminate()
      }.recover {
        case e: Throwable =>
          println(s"Unexpected error binding [${handler.path}] to TCP: ${e.getMessage}, shutting down.")
          system.terminate()
      }
    }
    val response = IO(Tcp).ask(Tcp.Bind(handler = handler, new InetSocketAddress(interface, port)))
    shutdownIfNotBound(response)
  }
}
