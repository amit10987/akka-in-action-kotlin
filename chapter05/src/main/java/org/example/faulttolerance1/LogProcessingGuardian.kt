package org.example.faulttolerance1

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class LogProcessingGuardian private constructor(
    val sources: List<String>,
    val databaseUrl: String,
    context: ActorContext<Nothing>
) : AbstractBehavior<Nothing>(context) {

    companion object {
        fun create(sources: List<String>, databaseUrl: String): Behavior<Nothing> {
            return Behaviors.setup { context ->
                sources.forEach { source ->
                    val dbWriter: ActorRef<DBWriter.Command> =
                        context.spawnAnonymous(DBWriter.create(databaseUrl))
                    val logProcessor: ActorRef<LogProcessor.Command> =
                        // wouldn't it be better to have more log processors
                        context.spawnAnonymous(LogProcessor.create(dbWriter))

                    val fileWatcher: ActorRef<FileWatcher.Command> =
                        context.spawnAnonymous(FileWatcher.create(source, logProcessor))
                    context.watch(fileWatcher)
                }
                LogProcessingGuardian(sources, databaseUrl, context)
            }
        }
    }

    override fun createReceive(): Receive<Nothing> {
        return newReceiveBuilder().onAnyMessage { Behaviors.ignore() }.onSignal(Terminated::class.java) {
            context.log.info("Terminated")
            // checks there is some fileWatcher running
            // if there's no fileWatcher left
            //then shutsdown the system
            Behaviors.same()
        }.build()
    }
}