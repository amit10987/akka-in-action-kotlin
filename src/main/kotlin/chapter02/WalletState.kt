package chapter02

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors

object WalletState {
    sealed class Command {
        data class Increase(val amount: Int) : Command()
        data class Decrease(val amount: Int) : Command()
    }

    fun receiver(total: Int, max: Int): Behavior<Command> =
        Behaviors.receive { context, command ->
            when (command) {
                is Command.Increase -> {
                    val current = total + command.amount
                    if (current <= max) {
                        context.log.info("increasing to $current")
                        receiver(current, max)
                    } else {
                        context.log.info("I'm overloaded. Counting '$current' while max is '$max.Stopping")
                        Behaviors.stopped()
                    }
                }

                is Command.Decrease -> {
                    val current = total - command.amount
                    if (current < 0) {
                        context.log.info("Can't run below zero. Stopping.")
                        Behaviors.stopped()
                    } else {
                        context.log.info("decreasing to $current")
                        receiver(current, max)
                    }
                }
            }
        }
}