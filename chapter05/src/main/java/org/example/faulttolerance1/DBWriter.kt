package org.example.faulttolerance1

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.PreRestart
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.example.faulttolerance1.exception.DbBrokenConnectionException
import org.example.faulttolerance1.exception.DbNodeDownException

class DBWriter private constructor(context: ActorContext<Command>) : AbstractBehavior<DBWriter.Command>(context) {
    sealed interface Command {
        data class Line(
            val time: Long,
            val message: String,
            val messageType: String
        ) : Command
    }

    companion object {
        fun create(databaseUrl: String): Behavior<Command> =
            superviseStrategy(
                Behaviors.setup { context -> DBWriter(context) }
            )

        private fun superviseStrategy(beh: Behavior<Command>): Behavior<Command> = Behaviors.supervise(
            Behaviors.supervise(
                beh
            ).onFailure(DbBrokenConnectionException::class.java, SupervisorStrategy.restart())
        ).onFailure(DbNodeDownException::class.java, SupervisorStrategy.stop())

    }

    override fun createReceive(): Receive<Command> {
       return newReceiveBuilder()
           .onMessage(Command.Line::class.java) { TODO() }
           .onSignal(PostStop::class.java){
               context.log.info("DBWriter stopped")
               Behaviors.same()}
           .onSignal(PreRestart::class.java){
               context.log.info("DBWriter restarting")
               Behaviors.same()}
           .build()
    }
}

