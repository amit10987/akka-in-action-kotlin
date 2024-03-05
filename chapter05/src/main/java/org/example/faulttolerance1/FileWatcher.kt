package org.example.faulttolerance1

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import org.example.faulttolerance1.exception.CorruptedFileException
import java.io.File

class FileWatcher private constructor(
    private val directory: String,
    private val logProcessor: ActorRef<LogProcessor.Command>,
    context: ActorContext<Command>
) : AbstractBehavior<FileWatcher.Command>(context) {

    sealed interface Command {
        data class NewFile(val file: File, val timeAdded: Long) : Command
    }

    companion object {
        fun create(
            directory: String,
            logProcessor: ActorRef<LogProcessor.Command>
        ): Behavior<Command> =
            Behaviors.supervise(
                Behaviors.setup { context ->
                    FileWatcher(directory, logProcessor, context)
                }
            ).onFailure(CorruptedFileException::class.java, SupervisorStrategy.resume())
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Command.NewFile::class.java) { TODO() }
            .build()
    }
}