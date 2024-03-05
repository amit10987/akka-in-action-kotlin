import akka.actor.typed.ActorSystem
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.cluster.ClusterEvent
import akka.cluster.MemberStatus
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe
import akka.cluster.typed.Unsubscribe

class ClusterDomainEventListener(context: ActorContext<ClusterEvent.ClusterDomainEvent>) :
    AbstractBehavior<ClusterEvent.ClusterDomainEvent>(context) {
    companion object {
        fun create() =
            Behaviors.setup { context ->
                // Subscribe to ClusterDomainEvents
                Cluster.get(context.system).subscriptions()
                    .tell(Subscribe(context.self, ClusterEvent.ClusterDomainEvent::class.java))
                // Return a behavior that handles ClusterDomainEvents
                ClusterDomainEventListener(context)
            }

    }

    override fun createReceive(): Receive<ClusterEvent.ClusterDomainEvent> {
        return newReceiveBuilder()
            .onMessage(ClusterEvent.ClusterDomainEvent::class.java) {
                when (it) {
                    is ClusterEvent.MemberUp -> {
                        context.log.info("Member is Up: {}", it.member())
                        Behaviors.same()
                    }

                    is ClusterEvent.UnreachableMember -> {
                        context.log.info("Member detected as unreachable: {}", it.member())
                        Behaviors.same()
                    }

                    is ClusterEvent.MemberRemoved -> {
                        if (it.previousStatus() == MemberStatus.exiting()) {
                            context.log.info("Member $it gracefully exited, REMOVED.")
                        } else {
                            context.log.info("$it downed after unreachable, REMOVED.")
                        }
                        Behaviors.same()
                    }

                    is ClusterEvent.ReachableMember -> {
                        context.log.info("Member REACHABLE: {}", it.member())
                        Behaviors.same()
                    }

                    is ClusterEvent.MemberEvent -> {
                        Behaviors.same()
                    }

                    else -> {
                        Behaviors.unhandled()
                    }
                }
            }.onSignal(PostStop::class.java) {
                context.log.info("ClusterDomainEventListener stopped")
                Cluster.get(context.system).subscriptions().tell(
                    Unsubscribe(
                        context.self
                    )
                )
                Behaviors.stopped()
            }
            .build()
    }
}

fun main() {
    val system = ActorSystem.create(ClusterDomainEventListener.create(), "words")
    println("Press ENTER to terminate")
    readLine()
    system.terminate()
}