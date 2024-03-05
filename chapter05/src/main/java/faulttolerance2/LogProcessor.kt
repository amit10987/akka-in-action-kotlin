package faulttolerance2

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.example.faulttolerance1.exception.CorruptedFileException
import java.io.File

class LogProcessor private constructor(context: ActorContext<Command>) : AbstractBehavior<LogProcessor.Command>(context) {
    sealed interface Command {
        data class LogFile(val file: File) : Command
    }

    companion object {
        fun create(dbWriter: ActorRef<DbWriter.Command>): Behavior<Command> {
            return Behaviors.supervise(
                Behaviors.setup { context ->
                    LogProcessor(context)
                }
            ).onFailure(ParseException::class.java, SupervisorStrategy.resume())
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Command.LogFile::class.java) {
                context.log.info("Processing file: ${it.file.name}")
                //parses file and sends each line to dbWriter
                Behaviors.same()
            }.onSignal(Terminated::class.java) {
                context.log.info("DbWriter terminated")
                //recreate the dbWriter
                //or stop itself
                Behaviors.same()
            }
            .build()
    }
}