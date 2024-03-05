package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.Routers

class DataEnricher private constructor(
    context: ActorContext<Command>,
    private val router: ActorRef<Aggregator.Command>
) : AbstractBehavior<DataEnricher.Command>(context) {

    sealed interface Command

    data class Message(val id: String, val content: String) : Command

    companion object {
        fun create(): Behavior<Command> {
            return Behaviors.setup { context ->
                val router = context.spawnAnonymous(
                    Routers.group(Aggregator.serviceKey)
                        .withConsistentHashingRouting(10) { command -> Aggregator.mapping(command) }
                )

                DataEnricher(context, router)
            }
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Message::class.java) { message ->
                val (id, _) = message
                // Fetch some metadata and send Enriched command to router
                router.tell(Aggregator.Enriched(id, ":metadata"))
                Behaviors.same()
            }
            .build()
    }
}
