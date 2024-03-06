package projection

import SPContainer
import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.javadsl.JdbcHandler
import org.slf4j.LoggerFactory
import repository.CargosPerContainerRepository
import repository.ScalikeJdbcSession

class CPCProjectionHandler(private val repository: CargosPerContainerRepository) :
    JdbcHandler<EventEnvelope<SPContainer.Event>, ScalikeJdbcSession>() {
    val logger = LoggerFactory.getLogger(CPCProjectionHandler::class.java)
    override fun process(session: ScalikeJdbcSession, envelope: EventEnvelope<SPContainer.Event>) {
        if (envelope.event() is SPContainer.CargoAdded) {
            val event = envelope.event() as SPContainer.CargoAdded
            repository.addCargo(event.containerId, session)
        }
    }
}
