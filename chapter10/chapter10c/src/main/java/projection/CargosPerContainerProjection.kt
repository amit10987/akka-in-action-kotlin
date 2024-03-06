package projection

import SPContainer
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.query.javadsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.ProjectionId
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.javadsl.EventSourcedProvider
import akka.projection.javadsl.ExactlyOnceProjection
import akka.projection.jdbc.javadsl.JdbcProjection
import org.slf4j.LoggerFactory
import repository.CargosPerContainerRepository
import repository.ScalikeJdbcSession


class CargosPerContainerProjection(private val session: ScalikeJdbcSession) {
    val logger = LoggerFactory.getLogger(CargosPerContainerProjection::class.java)

    fun createProjectionFor(
        system: ActorSystem<Nothing>,
        repository: CargosPerContainerRepository,
        indexTag: Int,
    ): ExactlyOnceProjection<Offset, EventEnvelope<SPContainer.Event>> {
        val tag = "container-tag-$indexTag"
        val sourceProvider =
            EventSourcedProvider.eventsByTag<SPContainer.Event>(
                system,
                JdbcReadJournal.Identifier(),
                tag
            )
        val handler = CPCProjectionHandler(repository)
        return JdbcProjection.exactlyOnce(
            ProjectionId.of("CargosPerContainerProjection", tag),
            sourceProvider,
            { session },
            { handler },
            system
        )
    }
}