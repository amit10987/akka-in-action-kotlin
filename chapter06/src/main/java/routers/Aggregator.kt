package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey

class Aggregator private constructor(
    context: ActorContext<Command>,
    val messages: Map<String, String>,
    val forwardTo: ActorRef<Event>
) : AbstractBehavior<Aggregator.Command>(context) {

    companion object {
        val serviceKey: ServiceKey<Command> = ServiceKey.create(Command::class.java, "agg-key")

        fun create(
            messages: Map<String, String> = emptyMap(),
            forwardTo: ActorRef<Event>
        ): Behavior<Command> {
            return Behaviors.setup { context ->
                context.system.receptionist().tell(
                    Receptionist.register(serviceKey, context.self)
                )
                Aggregator(context, messages, forwardTo)
            }
        }

        fun mapping(command: Command): String = command.id
    }



    sealed interface Command {
        val id: String
    }

    data class Obfuscated(override val id: String, val content: String) : Command
    data class Enriched(override val id: String, val metadata: String) : Command

    sealed interface Event {
        val id: String
    }

    data class Completed(override val id: String, val content: String, val metadata: String) : Event


    private fun receiveMessages(
        messages: Map<String, String>,
        forwardTo: ActorRef<Event>
    ): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Obfuscated::class.java) { obfuscated ->
                val (id, content) = obfuscated
                val metadata = messages[id]
                if (metadata != null) {
                    forwardTo.tell(Completed(id, content, metadata))
                    receiveMessages(messages - id, forwardTo)
                } else {
                    receiveMessages(messages + (id to content), forwardTo)
                }
            }
            .onMessage(Enriched::class.java) { enriched ->
                val (id, metadata) = enriched
                val content = messages[id]
                if (content != null) {
                    forwardTo.tell(Completed(id, content, metadata))
                    receiveMessages(messages - id, forwardTo)
                } else {
                    receiveMessages(messages + (id to metadata), forwardTo)
                }
            }
            .build()
    }

    override fun createReceive(): Receive<Command> = receiveMessages(messages, forwardTo)
}
