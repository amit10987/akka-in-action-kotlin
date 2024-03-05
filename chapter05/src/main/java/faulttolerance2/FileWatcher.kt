package faulttolerance2

import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import java.io.File

class FileWatcher private constructor(actorContext: ActorContext<Command>) : FileListeningAbilities,
    AbstractBehavior<FileWatcher.Command>(actorContext) {

    sealed class Command {
        data class NewFile(val file: File, val timeAdded: Long) : Command()
        data class FileModified(val file: File, val timeAdded: Long) : Command()
    }

    companion object {
        fun create(directory: String): Behavior<Command> = Behaviors.setup { context ->
            //starts listening to directory, spawns and watches a LogProcessor
            //when new file in the directory it receives NewFile
            //when modified file in the directory it receives FileModified

            FileWatcher(context)
        }
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Command.NewFile::class.java) {
                context.log.info("New file: ${it.file.name}")
                Behaviors.same()
            }
            .onMessage(Command.FileModified::class.java) {
                context.log.info("Modified file: ${it.file.name}")
                Behaviors.same()
            }.onSignal(Terminated::class.java) {
                context.log.info("LogProcessor terminated")
                Behaviors.same()
            }
            .build()
    }

    override fun register(uri: String) {
        TODO("Not yet implemented")
    }

    // after restart work can get duplicated, deduplication is out of the scope.
}
