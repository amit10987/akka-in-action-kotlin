package routers

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.receptionist.ServiceKey

object PhotoProcessor {
    val key: ServiceKey<String> = ServiceKey.create(String::class.java, "photo-processor-key")

    fun create(): Behavior<String> {
        return Behaviors.ignore()
    }
}