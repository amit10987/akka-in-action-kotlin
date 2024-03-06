import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.Entity
import akka.http.javadsl.Http
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.Directives.complete
import akka.http.javadsl.server.Directives.concat
import akka.http.javadsl.server.Directives.*
import akka.http.javadsl.server.Route
import java.time.Duration

fun main(args: Array<String>) {
    val system = ActorSystem.create<Nothing>(Behaviors.empty(), "simple-api")

    val sharding = ClusterSharding.get(system)
    val shardingRegion =
        sharding.init(Entity.of(Container.TypeKey) { entityContext -> Container.create(entityContext.entityId) })

    val route: Route = path("cargo") {
        concat(post {
            parameterList {
                val entityId = it.find { param -> param.key == "entityId" }?.value as String
                val kind = it.find { param -> param.key == "kind" }?.value as String
                val size = it.find { param -> param.key == "size" }?.value as String
                val cargo = Container.Cargo(kind, size.toInt())
                shardingRegion.tell(
                    ShardingEnvelope(
                        entityId, Container.AddCargo(cargo)
                    )
                )
                complete(StatusCodes.ACCEPTED, "Adding Cargo requested")

            }
        }, get {
            parameter("entityId") { entityId ->
                val container = sharding.entityRefFor(Container.TypeKey, entityId)
                val response = container.ask({
                    Container.GetCargos(it)
                }, Duration.ofSeconds(5)).toCompletableFuture().get().cargos.toString()
                complete(
                    HttpResponse.create().withEntity(response)
                )
            }
        })
    }

    val bindingFuture = Http.get(system).newServerAt("localhost", 8080).bind(route)

    println("server at localhost:8080 \nPress RETURN to stop")
    readlnOrNull()
    bindingFuture.thenCompose { it.unbind() }.thenAccept { _ -> system.terminate() }
}