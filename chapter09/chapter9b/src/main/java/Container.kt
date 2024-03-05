import akka.actor.typed.ActorRef
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior



// Define Container class extending AbstractBehavior
class Container private constructor(
    val containerId: String
) : EventSourcedBehavior<Container.Command, Container.Event, Container.State>(PersistenceId.ofUniqueId(containerId)) {
    companion object {
        fun create(containerId: String) = Container(containerId)

    }

    // Define Cargo data class
    data class Cargo(val id: String, val kind: String, val size: Int)

    // Define sealed traits for Command and Event
    sealed interface Command
    data class AddCargo(val cargo: Cargo) : Command
    data class GetCargos(val replyTo: ActorRef<List<Cargo>>) : Command

    sealed interface Event
    data class CargoAdded(val containerId: String, val cargo: Cargo) : Event

    // Define State data class
    data class State(val cargos: List<Cargo> = emptyList())

    override fun emptyState(): State = State()

    override fun commandHandler(): CommandHandler<Command, Event, State> {
        return newCommandHandlerBuilder().forAnyState().onCommand(AddCargo::class.java) { state, command ->
            Effect().persist(CargoAdded(containerId, command.cargo))
        }.onCommand(GetCargos::class.java) { s, c ->
            Effect().none().thenRun { c.replyTo.tell(s.cargos) }
        }.build()

    }

    override fun eventHandler(): EventHandler<State, Event> {
        return newEventHandlerBuilder().forAnyState().onEvent(CargoAdded::class.java) { state, event ->
            state.copy(cargos = state.cargos + event.cargo)
        }.build()
    }

}
