import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.javadsl.Http
import akka.http.javadsl.server.Directives.complete
import akka.http.javadsl.server.Directives.get
import akka.http.javadsl.server.Directives.parameterList
import akka.http.javadsl.server.Directives.path
import akka.http.scaladsl.model.HttpResponse

fun main() {
    val system = ActorSystem.create<Nothing>(Behaviors.empty(), "simple-api")

    val route = path("validate") {
        get {
            parameterList { params ->
                val message = (params.find { param -> param.key == "quantity" }?.value as String).toInt()
                val accepted = if (message > 10)
                    Validated(false)
                else Validated(true)
                complete(HttpResponse.create().withEntity(accepted.toString()))
            }
        }
    }

    Http.get(system).newServerAt("127.0.0.1", 8080).bind(route)
}

data class Validated(val accepted: Boolean)