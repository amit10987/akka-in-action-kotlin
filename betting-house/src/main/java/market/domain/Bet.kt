package market.domain

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.TimerScheduler
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.EntityTypeKey
import akka.parboiled2.Parser.Mark
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.Effect
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior
import akka.persistence.typed.javadsl.ReplyEffect
import akka.persistence.typed.javadsl.RetentionCriteria
import java.time.Duration
import java.util.*
import kotlin.math.abs

class Bet private constructor(
    private val betId: String,
    private val context: ActorContext<Command>,
    private val timers: TimerScheduler<Command>,
) :
    EventSourcedBehavior<Bet.Command, Bet.Event, Bet.State>(
        PersistenceId.ofUniqueId(betId), SupervisorStrategy.restartWithBackoff(
            Duration.ofSeconds(10), Duration.ofSeconds(60), 0.1
        )
    ) {

    companion object {
        val tags = (0 until 3).map { "bet-tag-$it" }


        val typeKey = EntityTypeKey.create(Command::class.java, "bet")
        fun create(betId: String): Behavior<Command> =
            Behaviors.setup { context -> Behaviors.withTimers { timer -> Bet(betId, context, timer) } }

    }


    sealed interface Command : CborSerializable {
        interface ReplyCommand : Command {
            val replyTo: ActorRef<Response>

            data class Open(
                val walletId: String,
                val marketId: String,
                val odds: Double,
                val stake: Int,
                val result: Int, //0 winHome, 1 winAway, 2 draw
                override val replyTo: ActorRef<Response>,
            ) : ReplyCommand

            data class Settle(
                val result: Int,
                override val replyTo: ActorRef<Response>,
            ) : ReplyCommand

            data class Cancel(
                val reason: String,
                override val replyTo: ActorRef<Response>,
            ) : ReplyCommand

            data class GetState(
                override val replyTo: ActorRef<Response>,
            ) : ReplyCommand
        }

        data class MarketOddsAvailable(
            val available: Boolean,
            val marketOdds: Double?,
        ) : Command

        data class RequestWalletFunds(
            val response: Wallet.Response.UpdatedResponse,
        ) : Command

        data class ValidationsTimedOut(
            val seconds: Int,
        ) : Command

        data class Fail(
            val reason: String,
        ) : Command

        data class Close(
            val reason: String,
        ) : Command
    }

    sealed interface Response : CborSerializable {
        object Accepted : Response
        data class RequestUnaccepted(
            val reason: String,
        ) : Response

        data class CurrentState(
            val state: State,
        ) : Response
    }

    sealed interface Event : CborSerializable

    data class MarketConfirmed(val state: State.OpenState) : Event
    data class FundsGranted(val state: State.OpenState) : Event
    data class ValidationsPassed(val state: State.OpenState) : Event
    data class Opened(
        val betId: String,
        val walletId: String,
        val marketId: String,
        val odds: Double,
        val stake: Int,
        val result: Int,
    ) : Event, CborSerializable

    data class Settled(val betId: String) : Event, CborSerializable
    data class Cancelled(val betId: String, val reason: String) : Event, CborSerializable
    data class Failed(val betId: String, val reason: String) : Event, CborSerializable
    object Closed : Event, CborSerializable

    data class Status(
        val betId: String,
        val walletId: String,
        val marketId: String,
        val odds: Double,
        val stake: Int,
        val result: Int,
    ) : CborSerializable {
        companion object {
            fun empty(marketId: String): Status =
                Status(marketId, "uninitialized", "uninitialized", -1.0, -1, 0)
        }
    }

    sealed interface State : CborSerializable {
        val status: Status

        data class UninitializedState(
            override val status: Status,
        ) : State

        data class OpenState(
            override val status: Status,
            val marketConfirmed: Boolean? = null,
            val fundsConfirmed: Boolean? = null,
        ) : State // the ask user when market no longer available

        data class SettledState(
            override val status: Status,
        ) : State

        data class CancelledState(
            override val status: Status,
        ) : State

        data class FailedState(
            override val status: Status,
            val reason: String,
        ) : State

        data class ClosedState(
            override val status: Status,
        ) : State

    }

    override fun emptyState(): State = State.UninitializedState(Status.empty(betId))

    override fun commandHandler(): CommandHandler<Command, Event, State> {
        val builder = newCommandHandlerBuilder()
        builder.forStateType(State.UninitializedState::class.java)
            .onCommand(Command.ReplyCommand.Open::class.java) { state, command -> open(state, command) }

        builder.forStateType(State.OpenState::class.java)
            .onCommand(Command.MarketOddsAvailable::class.java) { state, command ->
                validateMarket(state, command)
            }
            .onCommand(Command.RequestWalletFunds::class.java) { state, command ->
                validateFunds(state, command)
            }
            .onCommand(Command.ValidationsTimedOut::class.java) { state, command ->
                checkValidations(state, command)
            }
            .onCommand(Command.ReplyCommand.Settle::class.java) { state, command ->
                settle(state, command)
            }
            .onCommand(Command.Close::class.java) { _, _ ->
                Effect().persist(Closed)
            }

        builder.forAnyState().onCommand(Command.ReplyCommand.GetState::class.java) { state, command ->
            Effect().none().thenReply(command.replyTo) { Response.CurrentState(state) }
        }

        return builder.build()
    }

    private fun settle(
        state: State,
        command: Command.ReplyCommand.Settle,
    ): Effect<Event, State> {
        val sharding = ClusterSharding.get(context.system)
        if (state.status.result == command.result) {
            val walletRef = sharding.entityRefFor(Wallet.typeKey, state.status.walletId)

            context.ask(
                Wallet.Response.UpdatedResponse::class.java,
                walletRef,
                Duration.ofSeconds(10),
                { Wallet.Command.AddFunds(state.status.stake, it) }) { res, ex ->
                res?.let {
                    Command.Close("stake reimbursed to wallet [$walletRef]")

                }?.let {
                    val message = "state NOT reimbursed to wallet [$walletRef]. Reason [${ex.message}]"
                    context.log.error(message)
                    Command.Fail(message)
                }
            }
        }
        return Effect().none()
    }


    private fun checkValidations(
        state: State.OpenState,
        command: Command.ValidationsTimedOut,
    ): Effect<Event, State> {
        return when {
            state.marketConfirmed == true && state.fundsConfirmed == true ->
                Effect().persist(ValidationsPassed(state))

            else ->
                Effect().persist(
                    Failed(
                        state.status.betId,
                        "validations didn't pass [$state]"
                    )
                )
        }
    }


    private fun validateMarket(
        state: State.OpenState,
        command: Command.MarketOddsAvailable,
    ): Effect<Event, State> {
        return if (command.available) {
            Effect().persist(MarketConfirmed(state))
        } else {
            Effect().persist(
                Failed(
                    state.status.betId,
                    "market odds [${command.marketOdds}] not available"
                )
            )
        }
    }

    private fun validateFunds(
        state: State.OpenState,
        command: Command.RequestWalletFunds,
    ): Effect<Event, State> {
        return when (command.response) {
            Wallet.Response.UpdatedResponse.Accepted -> Effect().persist(FundsGranted(state))
            Wallet.Response.UpdatedResponse.Rejected -> Effect().persist(
                Failed(
                    state.status.betId,
                    "funds not available"
                )
            )
        }
    }

    private fun open(
        state: State.UninitializedState,
        command: Command.ReplyCommand.Open,
    ): ReplyEffect<Event, State> {
        val sharding = ClusterSharding.get(context.system)
        timers.startSingleTimer(
            "lifespan",
            Command.ValidationsTimedOut(10), // this would read from configuration
            Duration.ofSeconds(10)
        )
        val open = Opened(
            state.status.betId,
            command.walletId,
            command.marketId,
            command.odds,
            command.stake,
            command.result
        )
        return Effect()
            .persist(open)
            .thenRun { requestMarketStatus(command, sharding, context) }
            .thenRun { requestFundsReservation(command, sharding, context) }
            .thenReply(command.replyTo) {
                Response.Accepted
            }
    }

    private fun requestFundsReservation(
        command: Command.ReplyCommand.Open,
        sharding: ClusterSharding,
        context: ActorContext<Command>,
    ) {
        val walletRef = sharding.entityRefFor(Wallet.typeKey, command.walletId)
        val walletResponseMapper =
            context.messageAdapter(Wallet.Response.UpdatedResponse::class.java) { Command.RequestWalletFunds(it) }

        walletRef.tell(Wallet.Command.ReserveFunds(command.stake, walletResponseMapper))
    }


    private fun requestMarketStatus(
        command: Command.ReplyCommand.Open,
        sharding: ClusterSharding,
        context: ActorContext<Command>,
    ) {
        val marketRef = sharding.entityRefFor(Market.typeKey, command.marketId)

        context.ask(
            Market.Response::class.java,
            marketRef,
            Duration.ofSeconds(3),
            { Market.Command.GetState(it) }) { response, exception ->
            (response as? Market.Response.CurrentState)?.let {
                val matched = oddsDoMatch(it.status, command)
                Command.MarketOddsAvailable(matched.doMatch, matched.marketOdds)
            }?.let {
                context.log.error(exception.message)
                Command.MarketOddsAvailable(false, 0.0)
            }
        }
    }

    data class Match(val doMatch: Boolean, val marketOdds: Double?)

    private fun oddsDoMatch(
        marketStatus: Market.Status,
        command: Command.ReplyCommand.Open,
    ): Match {
        // if better odds are available, the betting house takes the bet
        // for a lesser benefit to the betting customer. This is why we compare
        // with greater than or equal to (gte)

        return when {
            marketStatus.result == 0 -> {
                Match(
                    marketStatus.odds?.draw!! >= command.odds,
                    marketStatus.odds.draw
                )
            }

            marketStatus.result!! > 0 -> {
                Match(
                    marketStatus.odds?.winHome!! >= command.odds,
                    marketStatus.odds.winHome
                )
            }

            else -> {
                Match(
                    marketStatus.odds?.winAway!! >= command.odds,
                    marketStatus.odds.winAway
                )
            }
        }
    }


    override fun eventHandler(): EventHandler<State, Event> {
        val builder = newEventHandlerBuilder()
        builder.forAnyState()
            .onEvent(Opened::class.java) { _, event ->
                State.OpenState(
                    Status(event.betId, event.walletId, event.marketId, event.odds, event.stake, event.result),
                    marketConfirmed = null,
                    fundsConfirmed = null
                )
            }
            .onEvent(MarketConfirmed::class.java) { _, event ->
                event.state.copy(marketConfirmed = true)
            }
            .onEvent(FundsGranted::class.java) { _, event ->
                event.state.copy(fundsConfirmed = true)
            }
            .onEvent(ValidationsPassed::class.java) { _, event ->
                event.state
            }
            .onEvent(Closed::class.java) { state, _ ->
                State.ClosedState(state.status)
            }
            .onEvent(Settled::class.java) { state, _ ->
                State.SettledState(state.status)
            }
            .onEvent(Failed::class.java) { state, event ->
                State.FailedState(state.status, event.reason)
            }

        return builder.build()
    }

    override fun retentionCriteria(): RetentionCriteria {
        return RetentionCriteria.snapshotEvery(100, 2)
    }

    override fun tagsFor(event: Bet.Event): Set<String> {
        val tagIndex = abs(event.hashCode() % tags.size)
        return setOf(tags.elementAt(tagIndex))
    }
}