import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.receptionist.Receptionist
import java.time.Duration

class GuestFinder(context: ActorContext<Command>) : AbstractBehavior<GuestFinder.Command>(context) {

    companion object {
        fun create(): Behavior<Command> = Behaviors.setup { GuestFinder(it) }
    }

    sealed interface Command {
        data class Find(val actorName: String, val replyTo: ActorRef<ActorRef<VIPGuest.Command>>) : Command
        object Empty : Command
    }

    override fun createReceive(): Receive<Command> {

       return newReceiveBuilder().onMessage(Command.Find::class.java) {
            context.ask(
                Receptionist.Listing::class.java,
                context.system.receptionist(),
                Duration.ofSeconds(3),
                { actorRef -> Receptionist.find(HotelConcierge.goldenKey, actorRef) },
                { l, _ ->
                    if (l != null) {
                        l.getAllServiceInstances(HotelConcierge.goldenKey).filter { actorRef ->
                            actorRef.path().name().contains(it.actorName)
                        }.forEach { ar ->
                            it.replyTo.tell(ar)
                        }
                        Command.Empty
                    } else {
                        Command.Empty
                    }
                })
            Behaviors.same()
        }.build()
    }
}