import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.kafka.CommitterSettings
import akka.kafka.ConsumerSettings
import akka.kafka.Subscriptions
import akka.kafka.javadsl.Consumer
import akka.stream.javadsl.Sink
import org.apache.kafka.common.serialization.StringDeserializer

fun main() {
    val system = ActorSystem.create<Nothing>(Behaviors.empty(), "consumerOne")
    val config = system.settings().config().getConfig("akka.kafka.consumer")
    val consumerSettings =
        ConsumerSettings.create(config, StringDeserializer(), StringDeserializer())
            .withBootstrapServers("127.0.0.1:9092")
            .withGroupId("group01")
    Consumer.plainSource(consumerSettings, Subscriptions.topics("test"))
        .map { msg ->
            println("key = ${msg.key()}, value = ${msg.value()}, offset = ${msg.offset()}")
        }.runWith(Sink.ignore(), system)

    readlnOrNull()

    system.terminate()
}