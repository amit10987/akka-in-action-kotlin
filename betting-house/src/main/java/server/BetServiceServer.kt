package server

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.http.javadsl.Http
import akka.http.javadsl.ServerBinding
import entry.BetServiceImplSharding
import example.bet.grpc.BetServiceHandlerFactory
import java.util.concurrent.CompletionStage

class BetServiceServer {
    companion object {
        fun init(
            system: ActorSystem<*>,
            sharding: ClusterSharding,
        ): CompletionStage<ServerBinding> {

            val betService =
                BetServiceHandlerFactory.createWithServerReflection(BetServiceImplSharding(sharding), system)

            val port = system.settings().config().getInt("services.bet.port")
            val host = system.settings().config().getString("services.host")

            return Http.get(system).newServerAt(host, port).bind(betService)

        }
    }
}