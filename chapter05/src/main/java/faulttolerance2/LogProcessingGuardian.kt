package faulttolerance2

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class LogProcessingGuardian private constructor(
    val databaseUrl: String,
    context: ActorContext<Nothing>
) : AbstractBehavior<Nothing>(context) {

    companion object {
        fun create(directories: List<String>, databaseUrl: String): Behavior<Nothing> {
            return Behaviors.setup { context ->
                directories.forEach { directory ->
                    val fileWatcher: ActorRef<FileWatcher.Command> =
                        context.spawnAnonymous(FileWatcher.create(directory))
                    context.watch(fileWatcher)
                }
                LogProcessingGuardian(databaseUrl, context)
            }
        }
    }

    override fun createReceive(): Receive<Nothing> {
        return newReceiveBuilder().onMessage(Nothing::class.java) {
            Behaviors.ignore()
        }
            .onSignal(Terminated::class.java) {
                // checks not all fileWatcher had Terminated
                // if no fileWatcher left shuts down the system
                Behaviors.same()
            }.build()
    }
}