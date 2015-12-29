package io.synaptix.pantilt

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import spray.can.Http

object Main extends App with RequestTimeout with ShutdownIfNotBound {

  val config = ConfigFactory.load()
  val restHost = config.getString("rest.host")
  val restPort = config.getInt("rest.port")

  implicit val system = ActorSystem("pan-tilt")
  implicit val executionContext = system.dispatcher
  implicit val timeout = requestTimeout(config)

  val api = system.actorOf(Props(new RestApi(timeout)), "httpInterface")

  val response = IO(Http).ask(Http.Bind(listener = api, interface = restHost, port = restPort))
}

trait RequestTimeout {

  import scala.concurrent.duration._

  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("spray.can.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}

trait ShutdownIfNotBound {

  import scala.concurrent.{ExecutionContext, Future}

  def shutdownIfNotBound(f: Future[Any])
                        (implicit system: ActorSystem, ec: ExecutionContext) = {
    f.mapTo[Http.Event].map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) =>
        println(s"REST interface could not bind: ${cmd.failureMessage}, shutting down.")
        system.terminate()
    }.recover {
      case e: Throwable =>
        println(s"Unexpected error binding to HTTP: ${e.getMessage}, shutting down.")
        system.terminate()
    }
  }
}
