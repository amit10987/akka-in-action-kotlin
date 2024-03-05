import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.receptionist.Receptionist

class GuestSearch private constructor(
    private val actorName: String,
    private val replyTo: ActorRef<ActorRef<VIPGuest.Command>>, context: ActorContext<Command>
) : AbstractBehavior<GuestSearch.Command>(context) {

    companion object {
        fun create(
            actorName: String,
            replyTo: ActorRef<ActorRef<VIPGuest.Command>>
        ): Behavior<Command> = Behaviors.setup { GuestSearch(actorName, replyTo, it) }
    }

    sealed interface Command {
        object Find : Command
        data class ListingResponse(val listing: Receptionist.Listing) : Command
    }

    override fun createReceive(): Receive<Command> {
        val listingResponseAdapter =
            context.messageAdapter(Receptionist.Listing::class.java) { m -> Command.ListingResponse(m) }
        return newReceiveBuilder()
            .onMessage(Command.Find::class.java) {
                context.system.receptionist().tell(Receptionist.find(HotelConcierge.goldenKey, listingResponseAdapter))
                Behaviors.same()
            }.onMessage(Command.ListingResponse::class.java) { lr ->
                lr.listing.getAllServiceInstances(HotelConcierge.goldenKey).filter { actorRef ->
                    actorRef.path().name().contains(actorName)
                }.forEach { actor -> replyTo.tell(actor) }
                Behaviors.same()
            }.build()
    }
}