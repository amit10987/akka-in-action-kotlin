import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.javadsl.AkkaManagement
import com.typesafe.config.ConfigFactory

fun main(args: Array<String>) {
    val address = args[0]

    val config = ConfigFactory
        .parseString(
            """
        akka.remote.artery.canonical.hostname = "127.0.0.$address"
        akka.management.http.hostname = "127.0.0.$address"
        """
        )
        .withFallback(ConfigFactory.load())

    val system =
        ActorSystem.create<Nothing>(Behaviors.empty(), "testing-bootstrap", config)

    AkkaManagement.get(system).start()
    ClusterBootstrap.get(system).start()
}