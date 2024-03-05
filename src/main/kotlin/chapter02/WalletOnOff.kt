package chapter02

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors

object WalletOnOff {
    sealed class Command {
        data class Increase(val amount: Int) : Command()
        object Deactivate : Command()
        object Activate : Command()
    }

    fun receiver(): Behavior<Command> = activated(0)

    private fun activated(total: Int): Behavior<Command> {
        return Behaviors.receive { context, command ->
            when (command) {
                is Command.Increase -> {
                    val current = total + command.amount
                    context.log.info("increasing to $current")
                    activated(current)
                }
                Command.Deactivate -> {
                    context.log.info("Deactivating")
                    deactivated(total)
                }
                Command.Activate -> {
                    context.log.info("Already activated")
                    Behaviors.same()
                }
            }
        }
    }

    private fun deactivated(total: Int): Behavior<Command> {
        return Behaviors.receive { context, command ->
            when (command) {
                is Command.Increase -> {
                    context.log.info("I am deactivated, I can't accept money")
                    Behaviors.same()
                }
                Command.Deactivate -> {
                    context.log.info("Already deactivated")
                    Behaviors.same()
                }
                Command.Activate -> {
                    context.log.info("Activating")
                    activated(total)
                }
            }
        }
    }
}