import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class SupervisionExample(context: ActorContext<String>) : AbstractBehavior<String>(context) {

    companion object {
        fun create(): Behavior<String> = Behaviors.setup { context ->
            SupervisionExample(context)
        }
    }

    override fun createReceive(): Receive<String> {
        return newReceiveBuilder().onMessage(String::class.java) { message ->
            when (message) {
                "secret" -> {
                    context.log.info("granted")
                    Behaviors.same()
                }

                "stop" -> {
                    context.log.info("stopping")
                    Behaviors.stopped()
                }

                "recoverable" -> {
                    context.log.info("recoverable")
                    throw IllegalStateException()
                }

                "fatal" -> {
                    throw OutOfMemoryError()

                }

                else -> {
                    context.log.info("unknown message")
                    Behaviors.same()
                }
            }
        }.onSignal(PostStop::class.java) {
            context.log.info("cleaning up resources")
            Behaviors.same()
        }.build()

    }
}