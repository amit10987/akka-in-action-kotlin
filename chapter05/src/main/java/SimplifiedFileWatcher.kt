import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class SimplifiedFileWatcher private constructor(context: ActorContext<Command>) :
    AbstractBehavior<SimplifiedFileWatcher.Command>(context) {

    companion object {
        fun create(): Behavior<Command> = Behaviors.setup { context ->
            SimplifiedFileWatcher(context)
        }
    }

    sealed interface Command {
        data class Watch(val ref: ActorRef<String>) : Command
    }

    override fun createReceive(): Receive<Command> {
       return newReceiveBuilder()
           .onMessage(Command.Watch::class.java) {
               context.watch(it.ref)
               Behaviors.same()
           }.onSignal(Terminated::class.java) {
               context.log.info("terminated")
               Behaviors.same()
           }
           .build()
    }
}