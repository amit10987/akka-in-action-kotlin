package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class Worker private constructor(context: ActorContext<String>, val monitor: ActorRef<String>) : AbstractBehavior<String>(context) {

    companion object {
        fun create(monitor: ActorRef<String>) = Behaviors.setup{Worker(it, monitor)}
    }

    override fun createReceive(): Receive<String> {
       return newReceiveBuilder().onMessage(String::class.java) {
           monitor.tell(it)
           Behaviors.same()
       }.build()
    }
}