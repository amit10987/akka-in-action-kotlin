import akka.actor.typed.ActorRef
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.cluster.sharding.typed.javadsl.EntityTypeKey

class Container(context: ActorContext<Command>, val containerId: String) : AbstractBehavior<Container.Command>(context) {

    companion object {
        val typeKey =
            EntityTypeKey.create(Command::class.java, "container-type-key")
        fun create(containerId: String) = Behaviors.setup { Container(it, containerId) }
    }

    data class Cargo(val id: String, val kind: String, val size: Int)
    sealed interface Command {
        data class AddCargo(val cargo: Cargo) : Command, CborSerializable
        data class GetCargos(val replyTo: ActorRef<List<Cargo>>) : Command, CborSerializable
    }

    override fun createReceive(): Receive<Command> = doReceive(emptyList())

    private fun doReceive(cargos: List<Cargo>): Receive<Command> {
        return newReceiveBuilder().onMessage(Command.AddCargo::class.java){
            doReceive(cargos + it.cargo)
        }.onMessage(Command.GetCargos::class.java){
            it.replyTo.tell(cargos)
            Behaviors.same()
        }.build()
    }
}