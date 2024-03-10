package entry

import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.Entity
import example.bet.grpc.BetProto
import example.bet.grpc.BetService
import market.domain.Bet
import java.time.Duration
import java.util.concurrent.CompletionStage

class BetServiceImplSharding(private val sharding: ClusterSharding) : BetService {

    init {
        sharding.init(Entity.of(Bet.typeKey) { Bet.create(it.entityId) })
    }

    override fun open(`in`: BetProto.Bet): CompletionStage<BetProto.BetResponse> {
        val bet = sharding.entityRefFor(Bet.typeKey, `in`.betId)
        return bet.ask({
            Bet.Command.ReplyCommand.Open(
                `in`.walletId, `in`.marketId, `in`.odds, `in`.stake, `in`.result, it
            )
        }, Duration.ofSeconds(3)).handle { res, ex -> betProtoResponse(res, ex) }
    }

    override fun settle(`in`: BetProto.SettleMessage): CompletionStage<BetProto.BetResponse> {
        val bet = sharding.entityRefFor(Bet.typeKey, `in`.betId)
        return bet.ask({ Bet.Command.ReplyCommand.Settle(`in`.result, it) }, Duration.ofSeconds(3)).handle { res, ex ->
            betProtoResponse(res, ex)
        }
    }

    override fun cancel(`in`: BetProto.CancelMessage): CompletionStage<BetProto.BetResponse>? {
        val bet = sharding.entityRefFor(Bet.typeKey, `in`.betId)
        return bet.ask({ Bet.Command.ReplyCommand.Cancel(`in`.reason, it) }, Duration.ofSeconds(3)).handle { res, ex ->
            betProtoResponse(res, ex)
        }
    }

    private fun betProtoResponse(
        res: Bet.Response?,
        ex: Throwable,
    ): BetProto.BetResponse? {
        val builder = BetProto.BetResponse.newBuilder()
        return when (res) {
            is Bet.Response.Accepted -> builder.setMessage("initialized").build()
            is Bet.Response.RequestUnaccepted -> builder.setMessage("Action not happened because [${res.reason}]")
                .build()

            else -> builder.setMessage("Action not happened because [${ex.message}]").build()
        }
    }

    override fun getState(`in`: BetProto.BetId): CompletionStage<BetProto.Bet> {
        val bet = sharding.entityRefFor(Bet.typeKey, `in`.betId)
        return bet.ask({ Bet.Command.ReplyCommand.GetState(it) }, Duration.ofSeconds(3))
            .handle { res, ex ->
                val builder = BetProto.Bet.newBuilder()
                when (res) {
                    is Bet.Response.CurrentState -> builder
                        .setBetId(res.state.status.betId)
                        .setWalletId(res.state.status.walletId)
                        .setMarketId(res.state.status.marketId)
                        .setOdds(res.state.status.odds)
                        .setStake(res.state.status.stake)
                        .setResult(res.state.status.result)
                        .build()

                    else -> throw ex
                }
            }
    }
}