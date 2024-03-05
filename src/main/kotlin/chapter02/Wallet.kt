package chapter02

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive


object Wallet {
    fun intReceiver(): Behavior<Int> {
        return Behaviors.receive { context, message ->
            context.log.info("Received message: $message")
            Behaviors.same()
        }
    }
}

