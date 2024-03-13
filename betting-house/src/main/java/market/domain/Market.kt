package market.domain

import akka.actor.typed.ActorRef
import akka.actor.typed.SupervisorStrategy
import akka.cluster.sharding.typed.javadsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior
import akka.persistence.typed.javadsl.RetentionCriteria
import market.domain.Market.State.*
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.abs

class Market private constructor(private val marketId: String) :
    EventSourcedBehavior<Market.Command, Market.Event, Market.State>(
        PersistenceId.ofUniqueId(marketId), SupervisorStrategy.restartWithBackoff(
            Duration.ofSeconds(10), Duration.ofSeconds(60), 0.1
        )
    ) {

    companion object {
        val tags = (0 until 3).map { i -> "market-tag-$i" }
        val typeKey = EntityTypeKey.create(Command::class.java, "market")
        fun create(marketId: String) = Market(marketId)
    }

    data class Fixture(
        val id: String,
        val homeTeam: String,
        val awayTeam: String,
    ) : CborSerializable

    data class Odds(
        val winHome: Double,
        val winAway: Double,
        val draw: Double,
    ) : CborSerializable

    sealed interface Command : CborSerializable {
        val replyTo: ActorRef<Response>

        data class Open(
            val fixture: Fixture,
            val odds: Odds,
            val opensAt: OffsetDateTime,
            override val replyTo: ActorRef<Response>,
        ) : Command

        data class Update(
            val odds: Odds,
            val opensAt: OffsetDateTime?,
            val result: Int, // 1 = winHome, 2 = winAway, 0 = draw
            override val replyTo: ActorRef<Response>,
        ) : Command

        data class Close(
            override val replyTo: ActorRef<Response>,
        ) : Command

        data class Cancel(
            val reason: String,
            override val replyTo: ActorRef<Response>,
        ) : Command

        data class GetState(
            override val replyTo: ActorRef<Response>,
        ) : Command
    }


    sealed interface Response : CborSerializable {
        object Accepted : Response
        data class CurrentState(val status: Status) : Response
        data class RequestUnaccepted(val reason: String) : Response
    }

    sealed interface Event : CborSerializable {
        val marketId: String

        data class Opened(
            override val marketId: String,
            val fixture: Fixture,
            val odds: Odds,
        ) : Event

        data class Updated(
            override val marketId: String,
            val odds: Odds?,
            val result: Int?,
        ) : Event

        data class Closed(
            override val marketId: String,
            val result: Int?,
            val at: OffsetDateTime,
        ) : Event

        data class Cancelled(
            override val marketId: String,
            val reason: String,
        ) : Event

    }

    data class Status(
        val marketId: String,
        val fixture: Fixture,
        val odds: Odds?,
        val result: Int?,
    ) : CborSerializable {
        companion object {
            fun empty(marketId: String): Status =
                Status(marketId, Fixture("", "", ""), Odds(-1.0, -1.0, -1.0), 0)

        }
    }

    sealed interface State : CborSerializable {
        val status: Status

        data class UninitializedState(override val status: Status) : State
        data class OpenState(override val status: Status) : State
        data class ClosedState(override val status: Status) : State
        data class CancelledState(override val status: Status) : State
    }

    override fun emptyState(): State = UninitializedState(Status.empty(marketId))

    override fun commandHandler(): CommandHandler<Command, Event, State> {
        val builder = newCommandHandlerBuilder()
        builder.forStateType(UninitializedState::class.java)
            .onCommand(Command.Open::class.java) { state, command ->
                Effect().persist(Event.Opened(state.status.marketId, command.fixture, command.odds))
                    .thenReply(command.replyTo) { Response.Accepted }
            }
        builder.forStateType(OpenState::class.java).onCommand(Command.Update::class.java) { state, command ->
            Effect().persist(Event.Updated(state.status.marketId, command.odds, command.result))
                .thenReply(command.replyTo) { Response.Accepted }
        }.onCommand(Command.Close::class.java) { state, command ->
            Effect().persist(
                Event.Closed(
                    state.status.marketId,
                    state.status.result,
                    OffsetDateTime.now(ZoneId.of("UTC"))
                )
            ).thenReply(command.replyTo) { Response.Accepted }
        }

        builder.forAnyState().onCommand(Command.Cancel::class.java) { state, command ->
            Effect()
                .persist(Event.Cancelled(state.status.marketId, command.reason))
                .thenReply(command.replyTo) { Response.Accepted }
        }.onCommand(Command.GetState::class.java) { state, command ->
            Effect().none().thenReply(command.replyTo) { Response.CurrentState(state.status) }
        }

        builder.forAnyState().onAnyCommand { state, command ->
            Effect().none()
                .thenReply(command.replyTo) { Response.RequestUnaccepted("[$command] is not allowed upon state [$state]") }
        }

        return builder.build()
    }

    override fun eventHandler(): EventHandler<State, Event> {
        val builder = newEventHandlerBuilder()
        builder.forAnyState().onEvent(Event.Opened::class.java) { _, event ->
            OpenState(Status(marketId, event.fixture, event.odds, 0))
        }
        builder.forStateType(OpenState::class.java)
            .onEvent(Event.Updated::class.java) { state, event ->
                state.copy(status = Status(
                    state.status.marketId,
                    state.status.fixture,
                    event.odds?.let { state.status.odds },
                    event.result?.let { state.status.result }
                ))
            }.onEvent(Event.Closed::class.java) { state, event ->
                ClosedState(state.status.copy(result = event.result))
            }
        builder.forAnyState().onEvent(Event.Cancelled::class.java) { state, _ ->
            CancelledState(state.status)
        }

        return builder.build()
    }

    override fun retentionCriteria(): RetentionCriteria {
        return RetentionCriteria.snapshotEvery(100, 2)
    }

    override fun tagsFor(event: Market.Event): Set<String> {
        val tagIndex = abs(event.hashCode() % tags.size)
        return setOf(tags.elementAt(tagIndex))
    }
}