package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.PoolRouter
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.Routers

class BroadcastingManager private constructor(context: ActorContext<String>) :
    AbstractBehavior<String>(context) {

    companion object {
        fun create(behavior: Behavior<String>) = Behaviors.setup { context ->
            val poolSize = 4

            val routingBehavior: PoolRouter<String> =
                Routers
                    .pool(poolSize, behavior)
                    .withBroadcastPredicate { msg -> msg.length > 5 }

            val router: ActorRef<String> =
                context.spawn(routingBehavior, "test-pool")

            (0..10).forEach { _ ->
                router.tell("hi, there")
            }

            BroadcastingManager(context)
        }
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder().build()
    }
}
