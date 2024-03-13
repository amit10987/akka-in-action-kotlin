package projection.to.db

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.ProjectionBehavior
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.javadsl.EventSourcedProvider
import akka.projection.javadsl.ExactlyOnceProjection
import akka.projection.jdbc.javadsl.JdbcProjection
import market.domain.Bet
import projection.db_connection.ScalikeJdbcSession
import java.util.*

object BetProjection {
    fun init(system: ActorSystem<*>, repository: BetRepository) {
        ShardedDaemonProcess.get(system).init(
            ProjectionBehavior.Command::class.java,
            "BetProjection",
            Bet.tags.size,
            { index -> ProjectionBehavior.create(createProjection(system, repository, index)) },
            ShardedDaemonProcessSettings.create(system),
            Optional.of(ProjectionBehavior.stopMessage())
        )
    }

    private fun createProjection(
        system: ActorSystem<*>,
        repository: BetRepository,
        index: Int,
    ): ExactlyOnceProjection<Offset, EventEnvelope<Bet.Event>> {
        val tag = Bet.tags[index]

        val sourceProvider = EventSourcedProvider.eventsByTag<Bet.Event>(
            system,
            JdbcReadJournal.Identifier(),
            tag
        )

        return JdbcProjection.exactlyOnce(
            ProjectionId.of("BetProjection", tag),
            sourceProvider,
            { ScalikeJdbcSession() },
            { BetProjectionHandler(repository) },
            system
        )
    }
}
