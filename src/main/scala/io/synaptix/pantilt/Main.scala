package io.synaptix.pantilt

import java.net.{InetAddress, InetSocketAddress}

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member}
import akka.io.{IO, Tcp}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import io.synaptix.pantilt.RestApi.RegisterRobotController
import spray.can.Http

object Main extends App with RequestTimeout with HttpBinding with TcpBinding {

  private val hostName: String = InetAddress.getLocalHost.getHostName

  private val config: Config = ConfigFactory
    .parseString(s"akka.remote.netty.tcp.hostname=$hostName")
    .withFallback(ConfigFactory.load())

  val restHost = config.getString("rest.host")
  val restPort = config.getInt("rest.port")
  val robotHost = config.getString("robot.host")
  val robotPort = config.getInt("robot.port")

  implicit val system = ActorSystem("PanTiltSystem")
  implicit val executionContext = system.dispatcher
  implicit val timeout = requestTimeout(config)

  val robotController = system.actorOf(RobotController.props, RobotController.name)

  val restApi = system.actorOf(RestApi.props, RestApi.name)
  bindHttpListener(restApi, restHost, restPort)

  restApi ! RegisterRobotController(robotController)

  val robotTcpServer = system.actorOf(RobotTcpServer.props(robotController), RobotTcpServer.name)
  bindTcpServer(robotTcpServer, robotHost, robotPort)

  val clusterListener = system.actorOf(Props(new Actor() with ActorLogging {

    implicit val cluster = Cluster(context.system)

    override def preStart(): Unit = {
      log.info(s"******** akka.remote.netty.tcp.hostname=$hostName")
      cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
        classOf[MemberEvent], classOf[UnreachableMember])
    }

    override def postStop(): Unit = {
      cluster.unsubscribe(self)
    }

    private def restApiSelection(member: Member): ActorSelection = {
      context.actorSelection(RootActorPath(member.address) / "user" / RestApi.name)
    }

    override def receive: Receive = {
      case MemberUp(newMember) =>
        log.info("Member is Up: {}", newMember.address)
        log.info("Registering RobotController with new member.")
        restApiSelection(newMember) ! RegisterRobotController(robotController)

      case UnreachableMember(member) =>
        log.info("Member detected as unreachable: {}", member)

      case MemberRemoved(removedMember, previousStatus) =>
        log.info("Member is Removed: {} after {}", removedMember.address, previousStatus)


      case _: MemberEvent => // ignore
    }
  }))

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
