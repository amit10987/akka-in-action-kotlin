import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.receptionist.Receptionist

class VIPGuest private constructor(context: ActorContext<Command>) : AbstractBehavior<VIPGuest.Command>(context) {

    companion object {
        fun create(): Behavior<Command> {
            return Behaviors.setup { context -> VIPGuest(context) }
        }
    }

    sealed interface Command {
        object EnterHotel : Command
        object LeaveHotel : Command
    }

    override fun createReceive(): Receive<Command> {
        return newReceiveBuilder()
            .onMessage(Command.EnterHotel::class.java) {
                context.system.receptionist().tell(Receptionist.register(HotelConcierge.goldenKey, context.self))
                Behaviors.same()
            }.onMessage(Command.LeaveHotel::class.java) {
                context.system.receptionist().tell(Receptionist.deregister(HotelConcierge.goldenKey, context.self))
                Behaviors.same()
            }.build()
    }
}