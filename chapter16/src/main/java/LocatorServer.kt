import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.javadsl.Http
import example.locator.grpc.LocatorServiceHandlerFactory

fun main(args: Array<String>) {
    val init = args[0].toInt()
    val system = ActorSystem.create<Nothing>(Behaviors.empty(), "LocatorServer")
    val service = LocatorServiceHandlerFactory.createWithServerReflection(LocatorServiceImpl(system, init), system)
    Http.get(system).newServerAt("0.0.0.0", 8080).bind(service)
}