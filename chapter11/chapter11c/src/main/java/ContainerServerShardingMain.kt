import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.Entity
import akka.http.javadsl.Http
import example.container.grpc.ContainerProto
import example.container.grpc.ContainerService
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

fun main() {
    val system = ActorSystem.create<Nothing>(Behaviors.empty(), "ContainerServer")
    val sharding: ClusterSharding = ClusterSharding.get(system)
    sharding.init(Entity.of(Container.TypeKey) { entityContext -> Container.create(entityContext.entityId) })
    val service =
        example.container.grpc.ContainerServiceHandlerFactory.createWithServerReflection(
            ContainerServiceSharedImpl(
                sharding
            ), system
        )

    val bindingFuture = Http.get(system).newServerAt("localhost", 8080).bind(service)

    println("server at localhost:8080 \nPress RETURN to stop")
    readlnOrNull()
    bindingFuture.thenCompose { it.unbind() }.thenAccept { _ -> system.terminate() }.toCompletableFuture().get()

}

class ContainerServiceSharedImpl(private val sharding: ClusterSharding) : ContainerService {
    override fun addCargo(`in`: ContainerProto.CargoEntity): CompletionStage<ContainerProto.AddedCargo> {
        val container = sharding.entityRefFor(Container.TypeKey, `in`.entityId)
        container.tell(Container.AddCargo(Container.Cargo(`in`.kind, `in`.size)))
        return CompletableFuture.completedFuture(ContainerProto.AddedCargo.newBuilder().setOk(true).build())
    }

    override fun getCargos(`in`: ContainerProto.EntityId): CompletionStage<ContainerProto.Cargos> {
        val container = sharding.entityRefFor(Container.TypeKey, `in`.entityId)
        return container.ask({
            Container.GetCargos(it)
        }, Duration.ofSeconds(5)).toCompletableFuture().thenApply {
            val cargos = it.cargos.map { cargo ->
                ContainerProto.Cargo.newBuilder().setKind(cargo.kind).setSize(cargo.size).build()
            }
            ContainerProto.Cargos.newBuilder().addAllCargo(cargos).build()
        }
    }
}