import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.kafka.ProducerSettings
import akka.kafka.javadsl.Producer
import akka.stream.javadsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class KafkaSpec {
    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `a producer should write to Kafka`() {
        val bootstrapServers = "127.0.0.1:9092"
        val config =
            testKit.system().settings().config().getConfig("akka.kafka.producer")

        val topicDest =
            testKit.system().settings().config().getString("kafka.test.topic")

        val producerSettings = ProducerSettings.create(
            config,
            StringSerializer(),
            StringSerializer()
        ).withBootstrapServers(bootstrapServers)

        val done = Source.range(1, 10)
            .map { it.toString() }.map { elem: String ->
                ProducerRecord<String, String>(topicDest, elem)
            }
            .runWith(Producer.plainSink(producerSettings), testKit.system())

        done.toCompletableFuture()

        Thread.sleep(3000)
    }


}