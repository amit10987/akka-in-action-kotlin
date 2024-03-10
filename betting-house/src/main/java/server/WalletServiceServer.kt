package server

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.http.javadsl.Http
import entry.WalletRoute

class WalletServiceServer {
    companion object {
        fun init(
            system: ActorSystem<*>,
            sharding: ClusterSharding,
        ) {
            val port =
                system.settings().config().getInt("services.wallet.port")
            val host = system.settings().config().getString("services.host")
            Http.get(system)
                .newServerAt(host, port)
                .bind(WalletRoute(sharding).createRoute())

        }
    }
}