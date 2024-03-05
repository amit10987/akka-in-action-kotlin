package chapter02

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors
import java.time.Duration

object WalletTimer {
    sealed class Command {
        data class Increase(val amount: Int) : Command()
        data class Deactivate(val seconds: Int) : Command()
        object Activate : Command()
    }

    fun receiver(): Behavior<Command> =
        activated(0)

    private fun activated(total: Int): Behavior<Command> =
        Behaviors.receive { context, command ->
            Behaviors.withTimers { timers ->
                when (command) {
                    is Command.Increase -> {
                        val current = total + command.amount
                        context.log.info("increasing to $current")
                        activated(current)
                    }

                    is Command.Deactivate -> {
                        timers.startSingleTimer(Command.Activate, Duration.ofSeconds(command.seconds.toLong()))
                        deactivated(total)
                    }

                    is Command.Activate -> Behaviors.same()
                }
            }
        }

    private fun deactivated(total: Int): Behavior<Command> =
        Behaviors.receive { context, command ->

            when (command) {
                is Command.Increase -> {
                    context.log.info("wallet is deactivated. Can't increase")
                    Behaviors.same()
                }

                is Command.Deactivate -> {
                    context.log.info(
                        "wallet is deactivated. Can't be deactivated again"
                    )
                    Behaviors.same()
                }

                is Command.Activate -> {
                    context.log.info("activating")
                    activated(total)
                }
            }

        }
}