package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.Routers

class Manager private constructor(context: ActorContext<String>) : AbstractBehavior<String>(context) {

    companion object {
        fun create(behavior: Behavior<String>) = Behaviors.setup { context ->
            val routingBehavior: Behavior<String> =
                Routers.pool(4, behavior)
            val router: ActorRef<String> =
                context.spawn(routingBehavior, "test-pool")

            (1..10).forEach { _ ->
                router.tell("hi")
            }
            Manager(context)
        }
    }

    override fun createReceive(): Receive<String> {
       return newReceiveBuilder().build()
    }
}