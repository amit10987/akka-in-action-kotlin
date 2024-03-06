import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.javadsl.Http
import akka.http.javadsl.server.Directives.complete
import akka.http.javadsl.server.Directives.concat
import akka.http.javadsl.server.Directives.*
import akka.http.javadsl.server.Route

fun main(args: Array<String>) {
    val system = ActorSystem.create<Nothing>(Behaviors.empty(), "simple-api")

    val route: Route =
        path("ping") {
            concat(
                get {
                    complete("pong")
                },
                post {
                    println("fake storing op")
                    complete("ping stored")
                })
        }

    val bindingFuture =
        Http.get(system)
            .newServerAt("localhost", 8080)
            .bind(route)

    println("server at localhost:8080 \nPress RETURN to stop")
    readlnOrNull()
    bindingFuture
        .thenCompose { it.unbind() }
        .thenAccept { _ -> system.terminate() }
        .toCompletableFuture()
        .get()
}