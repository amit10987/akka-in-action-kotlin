package projection.server

import akka.actor.typed.ActorSystem
import akka.http.javadsl.Http
import betting.house.projection.proto.BetProjectionServiceHandlerFactory
import projection.to.db.BetProjectionServiceImpl
import projection.to.db.BetRepository

object BetProjectionServer {

    fun init(repository: BetRepository, system: ActorSystem<*>) {
        val service =
            BetProjectionServiceHandlerFactory
                .create(BetProjectionServiceImpl(repository), system)

        val port = system.settings().config().getInt("services.bet-projection.port")
        val host = system.settings().config().getString("services.host")

        Http.get(system).newServerAt(host, port).bind(service)
    }
}