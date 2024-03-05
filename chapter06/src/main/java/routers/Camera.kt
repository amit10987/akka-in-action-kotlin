package routers

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.GroupRouter
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.Routers

class Camera private constructor(context: ActorContext<Photo>, private val router: ActorRef<String>) :
    AbstractBehavior<Camera.Photo>(context) {

    companion object {
        fun create(): Behavior<Photo> {
            return Behaviors.setup { context ->
                val routingBehavior: GroupRouter<String> = Routers.group(PhotoProcessor.key).withRoundRobinRouting()
                val router: ActorRef<String> = context.spawn(routingBehavior, "photo-processor-pool")
                Camera(context, router)
            }
        }
    }

    data class Photo(val content: String)

    override fun createReceive(): Receive<Photo> {
        return newReceiveBuilder()
            .onMessage(Photo::class.java) { message ->
                router.tell(message.content)
                Behaviors.same()
            }
            .build()
    }
}
