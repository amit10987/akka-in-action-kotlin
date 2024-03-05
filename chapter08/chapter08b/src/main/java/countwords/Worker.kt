package countwords

import akka.actor.AbstractActor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.event.Logging
import akka.event.LoggingAdapter


class Worker(context: ActorContext<Command>) : AbstractBehavior<Worker.Command>(context) {
    companion object {
        fun create(): Behavior<Command> = Behaviors.setup { context ->
            context.log.debug("${context.self} subscribing to $RegistrationKey")
            context.system.receptionist().tell(Receptionist.register(RegistrationKey, context.self))
            Worker(context)
        }

        val RegistrationKey = ServiceKey.create(Command::class.java, "worker")
    }

    sealed interface Command {
        data class Process(val text: String, val replyTo: ActorRef<Master.Event>) : Command, CborSerializable
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder().onMessage(Command.Process::class.java) { text ->
            context.log.debug("processing $text")
            text.replyTo.tell(Master.Event.CountedWords(processTask(text.text)))
            Behaviors.same()
        }.build()
    }

    fun processTask(text: String): Map<String, Int> {
        return text.split("\\W+".toRegex())
            .fold(emptyMap()) { mapAccumulator, word ->
                mapAccumulator + (word to (mapAccumulator.getOrDefault(word, 0) + 1))
            }
    }
}