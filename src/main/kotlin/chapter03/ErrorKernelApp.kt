package chapter03

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors

fun main() {
    val guardian: ActorSystem<ErrorKernelApp.Guardian.Command> =
        ActorSystem.create(ErrorKernelApp.Guardian.receiver(), "error-kernel")
    guardian.tell(
        ErrorKernelApp.Guardian.Command.Start(
            listOf(
                "a-b-c",
                "d-e-f",
                "g",
                "a-b",
                "a-bc",
                "d-ef",
                "-g",
                "a-b-",
                "ab-c",
                "de-f",
                "g-"
            )
        )
    )
}

object ErrorKernelApp {
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
            data class WorkerDoneAdapter(val result: Worker.Response) : Command
        }

        fun receiver(): Behavior<Command> =
            Behaviors.setup {
                val adapter: ActorRef<Worker.Response> = it.messageAdapter(Worker.Response::class.java) { response ->
                    Command.WorkerDoneAdapter(
                        response
                    )
                }

                Behaviors.receiveMessage { command ->
                    when (command) {
                        is Command.Delegate -> {
                            command.forms.forEach { form ->
                                val worker: ActorRef<Worker.Command> = it.spawn(Worker.receiver(), "worker-$form")
                                it.log.info("sending text '${form}' to worker")
                                worker.tell(Worker.Command.Parse(adapter, form))
                            }
                            Behaviors.same()
                        }

                        is Command.WorkerDoneAdapter -> {
                            it.log.info("text '${command.result}' has been finished")
                            Behaviors.same()
                        }
                    }

                }
            }
    }

    object Worker {
        sealed interface Command {
            data class Parse(val replyTo: ActorRef<Response>, val text: String) : Command
        }

        sealed interface Response {
            data class Done(val text: String) : Response
        }

        fun receiver(): Behavior<Command> =
            Behaviors.receive { context, command ->
                when (command) {
                    is Command.Parse -> {
                        context.log.info("parsing")
                        val parsed = naiveParsing(command.text)
                        context.log.info("'${context.self}' DONE!. Parsed result: $parsed")
                        command.replyTo.tell(Response.Done(command.text))
                        Behaviors.same()
                    }
                }
            }

        private fun naiveParsing(text: String): String {
            return text.replace("-", "")
        }
    }
}
