package market.domain

import akka.actor.typed.ActorRef
import akka.actor.typed.SupervisorStrategy
import akka.cluster.sharding.typed.javadsl.EntityTypeKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.javadsl.CommandHandler
import akka.persistence.typed.javadsl.EventHandler
import akka.persistence.typed.javadsl.EventSourcedBehavior
import akka.persistence.typed.javadsl.RetentionCriteria
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Duration
import kotlin.math.abs

class Wallet private constructor(private val walletId: String) :
    EventSourcedBehavior<Wallet.Command, Wallet.Event, Wallet.State>(
        PersistenceId.ofUniqueId(walletId), SupervisorStrategy.restartWithBackoff(
            Duration.ofSeconds(10), Duration.ofSeconds(60), 0.1
        )
    ) {
    companion object {
        val typeKey = EntityTypeKey.create(Command::class.java, "wallet")
        fun create(walletId: String) = Wallet(walletId)
    }

    sealed interface Event : CborSerializable {
        data class FundsReserved @JsonCreator constructor(
            @JsonProperty("amount") val amount: Int,
        ) : Event

        data class FundsAdded(val amount: Int) : Event
        data class FundsReservationDenied(val amount: Int) : Event
    }

    sealed interface Command : CborSerializable {
        data class ReserveFunds(
            val amount: Int,
            val replyTo: ActorRef<Response.UpdatedResponse>,
        ) : Command

        data class AddFunds(
            val amount: Int,
            val replyTo: ActorRef<Response.UpdatedResponse>,
        ) : Command

        data class CheckFunds(val replyTo: ActorRef<Response>) : Command
    }


    sealed interface Response : CborSerializable {
        sealed interface UpdatedResponse : Response {
            object Accepted : UpdatedResponse
            object Rejected : UpdatedResponse
        }

        data class CurrentBalance(val amount: Int) : Response
    }

    data class State(val balance: Int) : CborSerializable

    override fun emptyState(): State = State(0)

    override fun commandHandler(): CommandHandler<Command, Event, State> {
        return newCommandHandlerBuilder().forAnyState().onCommand(Command.ReserveFunds::class.java) { state, command ->
            if (state.balance >= command.amount) {
                Effect().persist(Event.FundsReserved(command.amount))
                    .thenReply(command.replyTo) { Response.UpdatedResponse.Accepted }
            } else {
                Effect().persist(Event.FundsReservationDenied(command.amount))
                    .thenReply(command.replyTo) { Response.UpdatedResponse.Rejected }
            }
        }.onCommand(Command.AddFunds::class.java) { _, command ->
            Effect().persist(Event.FundsAdded(command.amount))
                .thenReply(command.replyTo) { Response.UpdatedResponse.Accepted }
        }.onCommand(Command.CheckFunds::class.java) { state, command ->
            Effect().reply(command.replyTo, Response.CurrentBalance(state.balance))
        }.build()
    }

    override fun eventHandler(): EventHandler<State, Event> {
        return newEventHandlerBuilder().forAnyState().onEvent(Event.FundsReserved::class.java) { state, event ->
            State(state.balance - event.amount)
        }.onEvent(Event.FundsAdded::class.java) { state, event ->
            State(state.balance + event.amount)
        }.onEvent(Event.FundsReservationDenied::class.java) { state, _ ->
            state
        }.build()
    }

    override fun retentionCriteria(): RetentionCriteria {
        return RetentionCriteria.snapshotEvery(100, 2)
    }

    override fun tagsFor(event: Event): Set<String> {
        val tags = (0 until 3).map { i -> "wallet-tag-$i" }.toSet()
        val tagIndex = abs(walletId.hashCode() % tags.size)
        return setOf(tags.elementAt(tagIndex))
    }
}