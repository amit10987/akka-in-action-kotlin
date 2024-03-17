import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.dispatch.Envelope
import akka.kafka.CommitterSettings
import akka.kafka.ConsumerMessage
import akka.kafka.ConsumerSettings
import akka.kafka.ProducerMessage
import akka.kafka.ProducerSettings
import akka.kafka.Subscription
import akka.kafka.Subscriptions
import akka.kafka.javadsl.Consumer
import akka.kafka.javadsl.Producer
import akka.stream.javadsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer

fun main() {
    val system = ActorSystem.create<Nothing>(Behaviors.empty(), "producerOne")
    val config = system.settings().config().getConfig("akka.kafka.consumer")
    val consumerSettings =
        ConsumerSettings.create(config, StringDeserializer(), StringDeserializer())
            .withBootstrapServers("127.0.0.1:9092")
            .withGroupId("group01")
            .withProperty(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
            )

    val producerConfig =
        system.settings().config().getConfig("akka.kafka.producer")

    val committerSettings = CommitterSettings.create(system)

    val producerSettings = ProducerSettings.create(
        producerConfig,
        StringSerializer(),
        StringSerializer()
    ).withBootstrapServers("127.0.0.1:9092")

    val drainingControl = Consumer.committableSource(consumerSettings, Subscriptions.topics("test3"))
        .map { msg: ConsumerMessage.CommittableMessage<String, String> ->
            ProducerMessage.single<String, String, ConsumerMessage.Committable>(
                ProducerRecord(
                    "test4", msg.record().key(),
                    msg.record().value()
                ), msg.committableOffset()
            )
        }.toMat(
            Producer.committableSink(
                producerSettings,
                committerSettings
            ),
            Consumer::createDrainingControl
        ).run(system)
    readlnOrNull()

    val future = drainingControl.drainAndShutdown(system.executionContext())
    future.handle { _, _ ->
        system.terminate()
    }
}