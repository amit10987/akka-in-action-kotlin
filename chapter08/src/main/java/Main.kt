import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.management.javadsl.AkkaManagement

fun main(args: Array<String>) {
    val system = ActorSystem.create(Behaviors.empty<Void>(), "words")
    AkkaManagement.get(system).start()
}