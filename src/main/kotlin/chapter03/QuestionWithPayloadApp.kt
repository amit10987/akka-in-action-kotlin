package chapter03

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors
import java.time.Duration
import kotlin.random.Random

fun main() {
    val guardian: ActorSystem<QuestionWithPayloadApp.Guardian.Command> =
        ActorSystem.create(QuestionWithPayloadApp.Guardian.receiver(), "error-kernel")
    guardian.tell(
        QuestionWithPayloadApp.Guardian.Command.Start(
            listOf(
                "a-b-c",
                "d-e-f",
                "g",
            )
        )
    )
}

class QuestionWithPayloadApp {
    object Guardian {
        sealed interface Command {
            data class Start(val texts: List<String>) : Command
        }

        fun receiver(): Behavior<Command> =
            Behaviors.setup { context ->
                context.log.info("Setting up. Creating manager")
                val manager: ActorRef<Manager.Command> = context.spawn(Manager.receiver(), "manager-alpha")
                Behaviors.receiveMessage {
                    when (it) {
                        is Command.Start -> {
                            context.log.info("Starting. Delegating to manager")
                            manager.tell(Manager.Command.Delegate(it.texts))
                            Behaviors.same()
                        }
                    }

                }
            }
    }

    object Manager {
        sealed interface Command {
            data class Delegate(val forms: List<String>) : Command
            data class Report(val description: String) : Command
        }

        fun receiver(): Behavior<Command> =
            Behaviors.setup {

                Behaviors.receiveMessage { command ->
                    when (command) {
                        is Command.Delegate -> {
                            command.forms.forEach { form ->
                                val worker: ActorRef<Worker.Command> = it.spawn(Worker.receiver(), "worker-$form")
                                it.log.info("sending text '${form}' to worker")
                                it.ask(
                                    Worker.Response::class.java,
                                    worker,
                                    Duration.ofSeconds(3),
                                    { m -> Worker.Command.Parse(form, m) },
                                    { l, r ->
                                        if (l != null) {
                                            Command.Report("$form read by ${worker.path().name()}")
                                        } else {
                                            Command.Report("$form failed with ${r.message}")
                                        }
                                    })
                            }
                            Behaviors.same()
                        }

                        is Command.Report -> {
                            it.log.info("text '${command.description}' has been finished")
                            Behaviors.same()
                        }
                    }

                }
            }
    }

    object Worker {
        sealed interface Command {
            data class Parse(val text: String, val replyTo: ActorRef<Response>) : Command
        }

        sealed interface Response {
            object Done : Response
        }

        fun receiver(): Behavior<Command> =
            Behaviors.receive { context, command ->
                when (command) {
                    is Command.Parse -> {
                        context.log.info("parsing")
                        val parsed = fakeLengthyParsing(command.text)
                        context.log.info("'${context.self}' DONE!. Parsed result: $parsed")
                        command.replyTo.tell(Response.Done)
                        Behaviors.same()
                    }
                }
            }

        private fun fakeLengthyParsing(text: String) {
            val endTime = System.currentTimeMillis() + Random.nextInt(2000, 4000)
            while (endTime > System.currentTimeMillis()) {
            }
        }
    }
}

