import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.javadsl.Http
import akka.http.javadsl.model.ContentTypes
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.server.Directives.complete
import akka.http.javadsl.server.Directives.extractUri
import akka.http.javadsl.server.Directives.get
import akka.http.javadsl.server.Directives.handleExceptions
import akka.http.javadsl.server.Directives.path
import akka.http.javadsl.server.ExceptionHandler
import akka.http.javadsl.server.Route
import kotlin.random.Random

fun main(args: Array<String>) {
    val system = ActorSystem.create<Nothing>(Behaviors.empty(), "simple-api")

    val exceptionHandler = ExceptionHandler.newBuilder().match(ArithmeticException::class.java) { ex ->
        extractUri { uri ->
            complete(
                HttpResponse.create().withStatus(400)
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"error\": \"Sorry, something went wrong with $uri\"}")
            )
        }
    }.build()


    val route: Route =
        handleExceptions(exceptionHandler) {
            path("imfeelinglucky") {
                get {
                    complete(
                        HttpResponse.create().withEntity(
                            ContentTypes.APPLICATION_JSON, "exactly the site you wanted " +
                                    (Random.nextInt() / Random.nextInt(2))
                        )
                    )

                }
            }
        }

    val bindingFuture = Http.get(system).newServerAt("localhost", 8080).bind(route)

    println("server at localhost:8080 \nPress RETURN to stop")
    readlnOrNull()
    bindingFuture.thenCompose { it.unbind() }.thenAccept { _ -> system.terminate() }.toCompletableFuture().get()
}