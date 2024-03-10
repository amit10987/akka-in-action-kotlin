package entry

import akka.NotUsed
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.Entity
import akka.stream.javadsl.Source
import example.market.grpc.MarketProto
import example.market.grpc.MarketService
import market.domain.Market
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.CompletionStage

class MarketServiceImplSharding(private val sharding: ClusterSharding) : MarketService {

    init {
        sharding.init(Entity.of(Market.typeKey) { Market.create(it.entityId) })
    }

    override fun open(`in`: MarketProto.MarketData): CompletionStage<MarketProto.Response> {
        val market = sharding.entityRefFor(Market.typeKey, `in`.marketId)
        return market.ask({
            Market.Command.Open(
                Market.Fixture(
                    `in`.fixture.id,
                    `in`.fixture.homeTeam,
                    `in`.fixture.awayTeam
                ),
                Market.Odds(`in`.odds.winHome, `in`.odds.winAway, `in`.odds.tie),
                OffsetDateTime.ofInstant(Instant.ofEpochMilli(`in`.opensAt), ZoneOffset.UTC),
                it
            )
        }, Duration.ofSeconds(3)).handle { res, ex ->
            marketProtoResponse(res, ex)
        }
    }

    override fun update(`in`: Source<MarketProto.MarketData, NotUsed>?): Source<MarketProto.Response, NotUsed> {
        return `in`!!.mapAsync(10) { marketData ->
            val marketRef = sharding.entityRefFor(Market.typeKey, marketData.marketId)
            marketRef.ask({
                Market.Command.Update(
                    Market.Odds(
                        marketData.odds.winHome,
                        marketData.odds.winAway,
                        marketData.odds.tie
                    ),
                    OffsetDateTime.ofInstant(Instant.ofEpochMilli(marketData.opensAt), ZoneOffset.UTC),
                    marketData.resultValue,
                    it
                )
            }, Duration.ofSeconds(3)).handle { res, ex ->
                val builder = MarketProto.Response.newBuilder()
                when (res) {
                    is Market.Response.Accepted -> builder.setMessage("Updated").build()
                    is Market.Response.RequestUnaccepted -> builder.setMessage("Market NOT Updated because [${res.reason}]")
                        .build()

                    else -> builder.setMessage("Bet NOT Updated because [${ex.message}]").build()
                }
            }
        }
    }

    override fun closeMarket(`in`: MarketProto.MarketId): CompletionStage<MarketProto.Response> {
        val market = sharding.entityRefFor(Market.typeKey, `in`.marketId)
        return market.ask({ Market.Command.Close(it) }, Duration.ofSeconds(3)).handle { res, ex ->
            marketProtoResponse(res, ex)
        }
    }

    override fun cancel(`in`: MarketProto.CancelMarket): CompletionStage<MarketProto.Response> {
        val market = sharding.entityRefFor(Market.typeKey, `in`.marketId)
        return market.ask({ Market.Command.Cancel(`in`.reason, it) }, Duration.ofSeconds(3)).handle { res, ex ->
            marketProtoResponse(res, ex)
        }
    }

    override fun getState(`in`: MarketProto.MarketId): CompletionStage<MarketProto.MarketData> {
        val market = sharding.entityRefFor(Market.typeKey, `in`.marketId)
        return market.ask({ Market.Command.GetState(it) }, Duration.ofSeconds(3))
            .handle { res, ex ->
                val builder = MarketProto.MarketData.newBuilder()
                when (res) {
                    is Market.Response.CurrentState ->
                        builder.setMarketId(res.status.marketId)
                            .setFixture(
                                MarketProto.FixtureData.newBuilder().setId(res.status.fixture.id)
                                    .setAwayTeam(res.status.fixture.awayTeam).setHomeTeam(res.status.fixture.homeTeam)
                                    .build()
                            )
                            .setOdds(res.status.odds?.let {
                                MarketProto.OddsData.newBuilder().setWinHome(it.winHome).setWinAway(it.winAway)
                                    .setTie(it.draw).build()
                            }).setResult(res.status.result?.let { MarketProto.MarketData.Result.forNumber(it) })
                            .build()

                    else -> throw ex
                }
            }
    }

    private fun marketProtoResponse(
        res: Market.Response?,
        ex: Throwable,
    ): MarketProto.Response? {
        val builder = MarketProto.Response.newBuilder()
        return when (res) {
            is Market.Response.Accepted -> builder.setMessage("initialized").build()
            is Market.Response.RequestUnaccepted -> builder.setMessage("Action not happened because [${res.reason}]")
                .build()

            else -> builder.setMessage("Action not happened because [${ex.message}]").build()
        }
    }
}