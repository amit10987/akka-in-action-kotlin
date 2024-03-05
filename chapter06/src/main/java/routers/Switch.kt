package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class Switch private constructor(
    context: ActorContext<Command>,
    private val forwardTo: ActorRef<String>,
    private val alertTo: ActorRef<String>,
) : AbstractBehavior<Switch.Command>(context) {

    sealed interface Command
    object SwitchOn : Command
    object SwitchOff : Command
    data class Payload(val content: String, val metadata: String) : Command

    companion object {
        fun create(forwardTo: ActorRef<String>, alertTo: ActorRef<String>): Behavior<Command> =
            Behaviors.setup { context ->
                Switch(context, forwardTo, alertTo)
            }
    }

    override fun createReceive(): Receive<Command> = on(forwardTo, alertTo)

    private fun on(
        forwardTo: ActorRef<String>,
        alertTo: ActorRef<String>
    ): Receive<Command> {
        return newReceiveBuilder().onMessage(SwitchOn::class.java) {
            context.log.warn("sent SwitchOn but was ON already")
            Behaviors.same()
        }.onMessage(SwitchOff::class.java) {
            off(forwardTo, alertTo)
        }.onMessage(Payload::class.java) {
            forwardTo.tell(it.content)
            Behaviors.same()
        }.build()
    }


    private fun off(
        forwardTo: ActorRef<String>,
        alertTo: ActorRef<String>
    ): Receive<Command> {
        return newReceiveBuilder().onMessage(SwitchOn::class.java) {
            on(forwardTo, alertTo)
        }.onMessage(SwitchOff::class.java){
            context.log.warn("sent SwitchOff but was OFF already")
            Behaviors.same()
        }.build()
    }
}
