import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.kafka.ConsumerMessage
import akka.kafka.ConsumerSettings
import akka.kafka.ProducerMessage
import akka.kafka.ProducerSettings
import akka.kafka.Subscriptions
import akka.kafka.javadsl.Consumer
import akka.kafka.javadsl.Transactional
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer

fun main(args: Array<String>) {
    val transactionalId = "test"
    val bootstrapServers = "127.0.0.1:9092"

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

    val producerSettings = ProducerSettings.create(
        producerConfig,
        StringSerializer(),
        StringSerializer()
    ).withBootstrapServers(bootstrapServers)

    val drainingControl = Transactional.source(
        consumerSettings,
        Subscriptions.topics("test5"),
    ).map { msg: ConsumerMessage.TransactionalMessage<String, String> ->
        ProducerMessage.single(
            ProducerRecord(
                "test6",
                msg.record().key(),
                msg.record().value()
            ),
            msg.partitionOffset()
        )
    }.toMat(Transactional.sink(producerSettings, transactionalId), Consumer::createDrainingControl)
        .run(system)
    readlnOrNull()

    val future = drainingControl.drainAndShutdown(system.executionContext())
    future.handle { _, _ ->
        system.terminate()
    }
}