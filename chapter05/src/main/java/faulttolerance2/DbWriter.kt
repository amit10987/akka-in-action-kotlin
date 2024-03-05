package faulttolerance2

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.PreRestart
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

class DbWriter private constructor(context: ActorContext<Command>) : AbstractBehavior<DbWriter.Command>(context) {
    sealed interface Command {
        data class Line(
            val time: Long,
            val message: String,
            val messageType: String
        ) : Command
    }

    companion object {
        fun create(databaseUrl: String): Behavior<Command> =
            supervisorStrategy(
                Behaviors.setup { context ->
                    DbWriter(context)
                }
            )

        private fun supervisorStrategy(behavior: Behavior<Command>): Behavior<Command> =
            Behaviors
                .supervise(
                    Behaviors
                        .supervise(behavior)
                        .onFailure(
                            UnexpectedColumnsException::class.java,
                            SupervisorStrategy.resume()
                        )
                )
                .onFailure(
                    DbBrokenConnectionException::class.java,
                    SupervisorStrategy.restartWithBackoff(
                        FiniteDuration(3, TimeUnit.SECONDS),
                        FiniteDuration(30, TimeUnit.SECONDS),
                        0.1
                    )
                        .withResetBackoffAfter(FiniteDuration(15, TimeUnit.SECONDS))
                )

    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Command.Line::class.java) {
                context.log.info("Writing to database: ${it.message}")
                //transforms line to db schema
                //saves to db
                Behaviors.same()
            }.onSignal(PostStop::class.java) {
                context.log.info("DBWriter stopped")
                Behaviors.same()
            }.onSignal(PreRestart::class.java) {
                context.log.info("DBWriter restarting")
                Behaviors.same()
            }.build()
    }
}
