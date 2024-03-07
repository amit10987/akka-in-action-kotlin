import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.javadsl.Http
import example.container.grpc.ContainerProto
import example.container.grpc.ContainerProto.AddedCargo
import example.container.grpc.ContainerService
import java.util.concurrent.CompletionStage
import java.util.concurrent.CompletableFuture

fun main() {
    val system = ActorSystem.create<Nothing>(Behaviors.empty(), "ContainerServer")

    val service =
        example.container.grpc.ContainerServiceHandlerFactory.createWithServerReflection(ContainerServiceImpl(), system)

    val bindingFuture = Http.get(system).newServerAt("localhost", 8080).bind(service)

    println("server at localhost:8080 \nPress RETURN to stop")
    readlnOrNull()
    bindingFuture.thenCompose { it.unbind() }.thenAccept { _ -> system.terminate() }.toCompletableFuture().get()
}

class ContainerServiceImpl : ContainerService {
    override fun addCargo(`in`: ContainerProto.Cargo?): CompletionStage<ContainerProto.AddedCargo> {
      return CompletableFuture.completedFuture(AddedCargo.newBuilder().setOk(true).build())
    }

}