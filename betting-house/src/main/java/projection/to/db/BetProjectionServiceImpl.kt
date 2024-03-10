package projection.to.db

import betting.house.projection.proto.BetProjectionProto
import betting.house.projection.proto.BetProjectionService
import projection.db_connection.ScalikeJdbcSession
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class BetProjectionServiceImpl(
    private val betRepository: BetRepository
) : BetProjectionService {
    override fun getBetByMarket(`in`: BetProjectionProto.MarketIdsBet): CompletionStage<BetProjectionProto.SumStakes> {
        val builder = BetProjectionProto.SumStakes.newBuilder()
        val sumStakes = ScalikeJdbcSession().withSession { session ->
            betRepository
                .getBetPerMarketTotalStake(`in`.marketId, session)
                .map { each ->
                    builder.addSumstakes(
                        BetProjectionProto.SumStake.newBuilder().setTotal(each.sum).setResult(each.result).build()
                    )
                }
            builder.build()
        }
        return CompletableFuture.completedFuture(sumStakes)
    }
}
