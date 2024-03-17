import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.kafka.CommitterSettings
import akka.kafka.ConsumerSettings
import akka.kafka.Subscriptions
import akka.kafka.javadsl.Committer
import akka.kafka.javadsl.Consumer

import akka.stream.javadsl.Sink
import org.apache.kafka.common.serialization.StringDeserializer

fun main() {
    val system = ActorSystem.create<Nothing>(Behaviors.empty(), "consumerOne")
    val config = system.settings().config().getConfig("akka.kafka.consumer")
    val consumerSettings =
        ConsumerSettings.create(config, StringDeserializer(), StringDeserializer())
            .withBootstrapServers("127.0.0.1:9092")
            .withGroupId("group02")
    val committerSettings = CommitterSettings.create(system)
    val drainingControl = Consumer.committableSource(consumerSettings, Subscriptions.topics("test2"))
        .map { msg ->
            println("key = ${msg.record().key()}, value = ${msg.record().value()}, offset = ${msg.record().offset()}")
            msg.committableOffset()
        }.via(Committer.flow(committerSettings))
        .toMat(Sink.seq(), Consumer::createDrainingControl).run(system)
    readlnOrNull()

    val future = drainingControl.drainAndShutdown(system.executionContext())
    future.handle { _, _ ->
        system.terminate()
    }
}