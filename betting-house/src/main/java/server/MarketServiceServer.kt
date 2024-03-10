package server

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import entry.MarketServiceImplSharding
import example.market.grpc.MarketServiceHandlerFactory
import java.util.concurrent.CompletionStage

class MarketServiceServer {
    companion object {
        fun init(
            system: ActorSystem<*>,
            sharding: ClusterSharding,
        ): CompletionStage<ServerBinding> {
            val marketService =
                MarketServiceHandlerFactory.createWithServerReflection(MarketServiceImplSharding(sharding), system)

            val port = system.settings().config().getInt("services.bet.port")
            val host = system.settings().config().getString("services.host")

            return Http.get(system).newServerAt(host, port).bind(marketService)
        }
    }
}