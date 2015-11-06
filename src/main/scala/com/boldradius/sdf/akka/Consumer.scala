package com.boldradius.sdf.akka

import akka.actor._

import Consumer._
import akka.cluster.{MemberStatus, Member, Cluster}
import akka.cluster.ClusterEvent._
import scala.concurrent.duration._

class Consumer(args: Args) extends PdAkkaActor {
  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      register(member)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent => // ignore

    case req: Request => {
      findOrCreateSessionLog(req.sessionId) ! SessionLog.AppendRequest(req)
    }
  }

  def register(member: Member): Unit = {
    if (member.hasRole("producer")) {
      context.actorSelection(RootActorPath(member.address) / "user" / "producer") ! RequestProducer.ConsumerRegistration(self)
    }
  }

  protected def findOrCreateSessionLog(sessionId: Long): ActorRef = {
    context.child(sessionId.toString).getOrElse {
      createChild(ThrottlingActor.Args(SessionLog.Args(sessionId, args.statsActor), 1, 1 minute), Some(sessionId.toString))
    }
  }
}

object Consumer {
  case class Args(statsActor: ActorRef) extends PdAkkaActor.Args(classOf[Consumer])
}
