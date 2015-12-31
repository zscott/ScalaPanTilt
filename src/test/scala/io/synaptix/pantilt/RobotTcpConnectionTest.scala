package io.synaptix.pantilt

import akka.actor.{ActorSystem, PoisonPill, Terminated}
import akka.io.Tcp
import akka.testkit.{TestActorRef, TestKit, TestKitExtension, TestProbe}
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
      ignoreMsg({ case _: Tcp.Register => true })
      val expectedData = ByteString(10, 57)

      val robotTcpConnection = system.actorOf(RobotTcpConnection.props(testActor))
      robotTcpConnection ! MoveTo(10.5, 57.22)

      expectMsgPF() {
        case Tcp.Write(data, _) =>
          data must be(expectedData)
      }
    }

    "Parent notified when the underlying connection disconnects" in {
      import RobotController._
      import akka.pattern.gracefulStop
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

    "RobotTcpConnection terminates when underlying connection terminates" in {
      import RobotController._
      import akka.pattern.gracefulStop
      implicit val timeout = TestKitExtension.get(system).DefaultTimeout
      ignoreMsg({
        case _: RobotConnected => true
        case _: RobotDisconnected => true
      })

      val underlyingConnection = TestProbe()
      val robotTcpConnection = TestActorRef(RobotTcpConnection.props(underlyingConnection.ref), testActor)

      // We need the an actor to monitor the robotTcpConnection
      val supervisor = TestProbe()
      supervisor.watch(robotTcpConnection)

      // When the underlying connection is killed / dies
      val killResultFuture = gracefulStop(underlyingConnection.ref, 5 seconds, PoisonPill)
      Await.result(killResultFuture, 5 seconds)

      // We expect the RobotTcpConnection to terminate itself
      supervisor.expectMsgPF() {
        case Terminated(actorRef) =>
          actorRef must be(robotTcpConnection)
      }
    }

    "RobotTcpConnection terminates when underlying connection disconnects via ErrorClosed" in {
      import RobotController._
      implicit val timeout = TestKitExtension.get(system).DefaultTimeout
      ignoreMsg({
        case _: RobotConnected => true
        case _: RobotDisconnected => true
      })

      val underlyingConnection = TestProbe()
      val robotTcpConnection = TestActorRef(RobotTcpConnection.props(underlyingConnection.ref), testActor)

      // We need the an actor to monitor the robotTcpConnection
      val supervisor = TestProbe()
      supervisor.watch(robotTcpConnection)

      // When the underlying connection sends Tcp.ErrorClosed to RobotTcpConnection
      underlyingConnection.send(robotTcpConnection, Tcp.ErrorClosed("testing Tcp.ErrorClosed"))

      // We expect the RobotTcpConnection to terminate itself
      supervisor.expectMsgPF() {
        case Terminated(actorRef) =>
          actorRef must be(robotTcpConnection)
      }
    }

    "RobotTcpConnection terminates when underlying connection disconnects via PeerClosed" in {
      import RobotController._
      implicit val timeout = TestKitExtension.get(system).DefaultTimeout
      ignoreMsg({
        case _: RobotConnected => true
        case _: RobotDisconnected => true
      })

      val underlyingConnection = TestProbe()
      val robotTcpConnection = TestActorRef(RobotTcpConnection.props(underlyingConnection.ref), testActor)

      // We need the an actor to monitor the robotTcpConnection
      val supervisor = TestProbe()
      supervisor.watch(robotTcpConnection)

      // When the underlying connection sends Tcp.PeerClosed to RobotTcpConnection
      underlyingConnection.send(robotTcpConnection, Tcp.PeerClosed)

      // We expect the RobotTcpConnection to terminate itself
      supervisor.expectMsgPF() {
        case Terminated(actorRef) =>
          actorRef must be(robotTcpConnection)
      }
    }


  }

}
