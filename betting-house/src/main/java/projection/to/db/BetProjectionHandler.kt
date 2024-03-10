package projection.to.db

import akka.projection.eventsourced.EventEnvelope
import akka.projection.jdbc.javadsl.JdbcHandler

import market.domain.Bet
import org.slf4j.LoggerFactory
import projection.db_connection.ScalikeJdbcSession


class BetProjectionHandler(private val repository: BetRepository) :
    JdbcHandler<EventEnvelope<Bet.Event>, ScalikeJdbcSession>() {

    private val logger = LoggerFactory.getLogger(BetProjectionHandler::class.java)

    override fun process(session: ScalikeJdbcSession, envelope: EventEnvelope<Bet.Event>) {
        when (val event = envelope.event()) {
            is Bet.Opened -> {
                val (betId, walletId, marketId, odds, stake, result) = event
                repository.addBet(betId, walletId, marketId, odds, stake, result, session)
            }

            else -> logger.debug("Ignoring event {} in projection", event)
        }
    }
}