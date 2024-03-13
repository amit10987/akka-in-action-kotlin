import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.javadsl.AkkaManagement
import com.typesafe.config.ConfigFactory

fun main() {
    val system =
        ActorSystem.create<Nothing>(
            Behaviors.empty(),
            "testing-bootstrap13b",
            ConfigFactory.load())

    AkkaManagement.get(system).start()
    ClusterBootstrap.get(system).start()
}