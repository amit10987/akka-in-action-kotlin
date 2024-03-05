import akka.actor.typed.ActorRef
import akka.actor.typed.SupervisorStrategy
import akka.cluster.sharding.typed.javadsl.EntityTypeKey
import akka.persistence.journal.Tagged
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior
import akka.persistence.typed.javadsl.RetentionCriteria
import java.time.Duration

// Define Container class extending AbstractBehavior
class SPContainer private constructor(private val containerId: String) :
    EventSourcedBehavior<SPContainer.Command, SPContainer.Event, SPContainer.State>(
        PersistenceId.ofUniqueId(containerId),
        SupervisorStrategy.restartWithBackoff(
            Duration.ofSeconds(10), Duration.ofSeconds(60), 0.1)
    ) {

    companion object {
        val typeKey = EntityTypeKey.create(Command::class.java, "spcontainer-type-key")
        fun create(containerId: String) = SPContainer(containerId)
    }

    data class Cargo(val id: String, val kind: String, val size: Int)

    // Define sealed traits for Command and Event
    sealed interface Command : CborSerializable
    data class AddCargo(val cargo: Cargo) : Command
    data class GetCargos(val replyTo: ActorRef<List<Cargo>>) : Command

    sealed interface Event : CborSerializable
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

    override fun retentionCriteria(): RetentionCriteria {
        return RetentionCriteria.snapshotEvery(100, 2)
    }

    override fun tagsFor(event: Event?): Set<String> {
        return setOf("container-tag-" + containerId.toInt() % 3)
    }

}
