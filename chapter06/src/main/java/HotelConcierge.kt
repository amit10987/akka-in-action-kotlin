import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey

class HotelConcierge(context: ActorContext<HotelConcierge.Command>) :
    AbstractBehavior<HotelConcierge.Command>(context) {

    companion object {
        val goldenKey = ServiceKey.create(VIPGuest.Command::class.java, "concierge-key")

        fun create(): Behavior<Command> = Behaviors.setup { context ->
            val listingNotificationAdapter = context.messageAdapter(
                Receptionist.Listing::class.java
            ) { x -> Command.ListingResponse(x) }

            context.system.receptionist().tell(Receptionist.subscribe(goldenKey, listingNotificationAdapter))

            HotelConcierge(context)
        }
    }

    sealed interface Command {
        data class ListingResponse(val listing: Receptionist.Listing) : Command
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder().onMessage(Command.ListingResponse::class.java) {
            it.listing.getAllServiceInstances(goldenKey)
                .forEach { actor -> context.log.info("${actor.path().name()} is in") }
            Behaviors.same()
        }.build()
    }
}