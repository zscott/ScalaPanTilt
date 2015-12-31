package io.synaptix.pantilt

import akka.actor.{PoisonPill, ActorSystem}
import akka.io.Tcp
import akka.testkit.{TestActorRef, TestProbe, TestKit, TestKitExtension}
import akka.util.ByteString
import io.synaptix.akka.StopSystemAfterAll
import io.synaptix.pantilt.RobotController.MoveTo
import org.scalatest.{MustMatchers, WordSpecLike}
import scala.concurrent.Await
import scala.concurrent.duration._

class RobotTcpConnectionTest extends TestKit(ActorSystem("test-system"))
                                     with WordSpecLike
                                     with MustMatchers
                                     with StopSystemAfterAll {


  "A RobotTcpConnection" must {

    "register as a handler with the underlying tcp connection" in {
      implicit val timeout = TestKitExtension.get(system).DefaultTimeout
      val robotTcpConnection = system.actorOf(RobotTcpConnection.props(testActor))

      expectMsgPF() {
        case Tcp.Register(handler, keepOpenOnPeerClosed, useResumeWriting) =>
          handler must be(robotTcpConnection)
          keepOpenOnPeerClosed must be(false)
          useResumeWriting must be(true)
      }
    }

    "write pan and tilt coordinates to underlying tcp connection" in {
      implicit val timeout = TestKitExtension.get(system).DefaultTimeout
      val robotTcpConnection = system.actorOf(RobotTcpConnection.props(testActor))
      robotTcpConnection ! MoveTo(10.5, 57.22)
      val expectedData = ByteString(10, 57)

      receiveWhile() {
        case _ : Tcp.Register =>
      }

      expectMsgPF() {
        case Tcp.Write(data, _) =>
          data must be(expectedData)
      }
    }

    "Parent notified when the underlying connection disconnects" in {
      import akka.pattern.gracefulStop
      import RobotController._
      implicit val timeout = TestKitExtension.get(system).DefaultTimeout

      val parent = TestProbe()
      val underlyingConnection = TestProbe()

      val robotTcpConnection = TestActorRef(RobotTcpConnection.props(underlyingConnection.ref), parent.ref)

      val killResultFuture = gracefulStop(underlyingConnection.ref, 5 seconds, PoisonPill)
      Await.result(killResultFuture, 5 seconds)

      parent.expectMsgPF() {
        case RobotDisconnected(sender) =>
          sender must be(robotTcpConnection)
      }
    }


  }

}
