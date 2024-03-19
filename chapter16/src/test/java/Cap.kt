import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors

object Cap {
    data class Increment(val increment: Int, val replyTo: ActorRef<Int>)

    fun behaviour(current: Int, max: Int): Behavior<Increment> {
        return Behaviors.receive { context, message ->
            val newCount = current + message.increment
            if (newCount > max) {
                message.replyTo.tell(current)
                Behaviors.same()
            } else {
                message.replyTo.tell(newCount)
                behaviour(newCount, max)
            }
        }
    }
}