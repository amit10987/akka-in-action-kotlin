import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.ChildFailed
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class ParentWatcher private constructor(
    context: ActorContext<Command>,
    private val children: List<ActorRef<String>>,
    private val monitor: ActorRef<String>
) :
    AbstractBehavior<ParentWatcher.Command>(context) {

    sealed interface Command
    data class Spawn(val behavior: Behavior<String>) : Command
    object StopChildren : Command
    object FailChildren : Command

    companion object {
        fun create(monitor: ActorRef<String>, children: List<ActorRef<String>> = emptyList()): Behavior<Command> =
            Behaviors.setup { context ->
                ParentWatcher(context, children, monitor)
            }
        fun childBehaviour(): Behavior<String> {
            return Behaviors.receive { _, message ->
                when (message) {
                    "stop" -> Behaviors.stopped()
                    "exception" -> throw IndexOutOfBoundsException()
                    "error" -> throw OutOfMemoryError()
                    else -> Behaviors.same()
                }
            }
        }
    }



    override fun createReceive(): Receive<Command> = receive(monitor, children)

    fun receive(monitor: ActorRef<String>, children: List<ActorRef<String>>): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Spawn::class.java) {
                val child = context.spawnAnonymous(childBehaviour())
                context.watch(child)
                receive(monitor, children + child)
            }
            .onMessage(StopChildren::class.java) {
                children.forEach { child -> child.tell("stop") }
                Behaviors.same()
            }
            .onMessage(FailChildren::class.java) {
                children.forEach { child -> child.tell("exception") }
                Behaviors.same()
            }
            .onSignal(ChildFailed::class.java) {
                monitor.tell("childFailed")
                Behaviors.same()
            }
            .onSignal(Terminated::class.java) {
                monitor.tell("terminated")
                Behaviors.same()
            }
            .build()
    }

}