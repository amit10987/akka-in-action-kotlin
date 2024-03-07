import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.cluster.sharding.typed.javadsl.EntityTypeKey

class Container(val entityId: String, context: ActorContext<Command>) : AbstractBehavior<Container.Command>(context) {

    companion object {
        val TypeKey = EntityTypeKey.create(Command::class.java, "container")
        fun create(entityId: String) = Behaviors.setup { ctx -> Container(entityId, ctx) }
    }

    data class Cargo(val kind: String, val size: Int)
    data class Cargos(val cargos: List<Cargo>)

    sealed interface Command
    data class AddCargo(val cargo: Cargo) : Command
    data class GetCargos(val replyTo: ActorRef<Cargos>) : Command

    override fun createReceive(): Receive<Command> = doReceive(entityId, emptyList())

    private fun doReceive(entityId: String, cargos: List<Cargo>): Receive<Command> {
        return newReceiveBuilder().onMessage(AddCargo::class.java) { command ->
            val newCargos = cargos + command.cargo
            context.log.info("Adding cargo to container $entityId")
            doReceive(entityId, newCargos)
        }.onMessage(GetCargos::class.java) { command ->
            command.replyTo.tell(Cargos(cargos))
            Behaviors.same()
        }.build()
    }
}