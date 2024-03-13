package projection.to.kafka

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess
import akka.kafka.ProducerSettings
import akka.kafka.javadsl.SendProducer
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.ProjectionBehavior
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.javadsl.EventSourcedProvider
import akka.projection.javadsl.AtLeastOnceProjection
import akka.projection.jdbc.javadsl.JdbcProjection
import market.domain.Bet
import market.domain.Market
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import projection.db_connection.ScalikeJdbcSession
import java.util.*
import java.util.function.Supplier

object MarketProjection {
    fun init(system: ActorSystem<*>) {
        val producer = createProducer(system)
        val topic = system.settings().config().getString("kafka.market-projection.topic")

        ShardedDaemonProcess.get(system).init(
            ProjectionBehavior.Command::class.java,
            "MarketProjection",
            Market.tags.size,
            { index -> ProjectionBehavior.create(createProjection(system, topic, producer, index)) },
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage())
        )
    }

    private fun createProducer(system: ActorSystem<*>): SendProducer<String, ByteArray> {
        val producerSettings =
            ProducerSettings.create( //they look up on creation at "akka.kafka.producer" in .conf
                system,
                StringSerializer(),
                ByteArraySerializer()
            )
        val sendProducer = SendProducer(producerSettings, system)
        CoordinatedShutdown.get(system).addTask(
            CoordinatedShutdown.PhaseBeforeActorSystemTerminate(),
            "closing send producer",
            Supplier {
                sendProducer.close()
            }
        )
        return sendProducer
    }


    private fun createProjection(
        system: ActorSystem<*>,
        topic: String,
        producer: SendProducer<String, ByteArray>,
        index: Int,
    ): AtLeastOnceProjection<Offset, EventEnvelope<Market.Event>> {
        val tag = Market.tags[index]

        val sourceProvider = EventSourcedProvider.eventsByTag<Market.Event>(
            system,
            JdbcReadJournal.Identifier(),
            tag
        )

        return JdbcProjection.atLeastOnceAsync(
            ProjectionId.of("MarketProjection", tag),
            sourceProvider,
            { ScalikeJdbcSession() },
            { MarketProjectionHandler(system, topic, producer) },
            system
        )
    }
}