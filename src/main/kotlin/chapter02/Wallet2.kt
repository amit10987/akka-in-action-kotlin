package chapter02

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class Wallet2(context: ActorContext<Int>) : AbstractBehavior<Int>(context) {

    companion object {
        fun create(): Behavior<Int> {
            return Behaviors.setup { context -> Wallet2(context) }
        }
    }

    override fun createReceive(): Receive<Int> {
        return newReceiveBuilder().onAnyMessage { message ->
            context.log.info("Received message: $message")
            Behaviors.same()
        }.build()
    }
}

fun main() {
    val guardian = ActorSystem.create(Wallet2.create(), "IntReceiverSystem")
    guardian.tell(2)
    guardian.tell(11)
    println("Press ENTER to terminate")
    readlnOrNull()
    guardian.terminate()
}