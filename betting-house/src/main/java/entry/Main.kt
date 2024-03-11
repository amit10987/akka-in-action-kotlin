package entry

import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.javadsl.AkkaManagement
import org.slf4j.LoggerFactory
import projection.db_connection.ScalikeJdbcSetup
import projection.server.BetProjectionServer
import projection.to.db.BetProjection
import projection.to.db.BetRepositoryImpl
import projection.to.kafka.MarketProjection
import server.BetServiceServer
import server.MarketServiceServer
import server.WalletServiceServer

fun main() {
    val system = ActorSystem.create(Behaviors.empty<Nothing>(), "betting-house")
    val log = LoggerFactory.getLogger("Main")
    try {
        val sharding = ClusterSharding.get(system)
        AkkaManagement.get(system).start()
        ClusterBootstrap.get(system).start()
        ScalikeJdbcSetup(system)

        BetServiceServer.init(system, sharding)
        MarketServiceServer.init(system, sharding)
        WalletServiceServer.init(system, sharding)

        val betRepository = BetRepositoryImpl()
        BetProjectionServer.init(betRepository, system)
        BetProjection.init(system, betRepository)
        MarketProjection.init(system)
    } catch (ex: Exception) {
        log.error(
            "Terminating Betting App. Reason [${ex.message}]"
        )
        system.terminate()
    }


}