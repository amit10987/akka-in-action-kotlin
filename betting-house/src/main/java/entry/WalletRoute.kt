package entry

import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.Entity
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.Directives.complete
import akka.http.javadsl.server.Directives.concat
import akka.http.javadsl.server.Directives.get
import akka.http.javadsl.server.Directives.parameterList
import akka.http.javadsl.server.Directives.path
import akka.http.javadsl.server.Directives.pathPrefix
import akka.http.javadsl.server.Directives.post
import akka.http.javadsl.server.Route
import market.domain.Wallet
import java.time.Duration

class WalletRoute(private val sharding: ClusterSharding) {
    init {
        sharding.init(Entity.of(Wallet.typeKey) { entityContext -> Wallet.create(entityContext.entityId) })
    }

    fun createRoute(): Route =
        pathPrefix("wallet") {
            concat(
                path("add") {
                    post {
                        parameterList { params ->
                            val walletId = params.find { param -> param.key == "walletId" }?.value as String
                            val funds = params.find { param -> param.key == "funds" }?.value as String

                            val walletRef = sharding.entityRefFor(Wallet.typeKey, walletId)

                            val response =
                                walletRef.ask({ Wallet.Command.AddFunds(funds.toInt(), it) }, Duration.ofSeconds(3))
                                    .toCompletableFuture().get() as Wallet.Response.UpdatedResponse.Accepted

                            complete(HttpResponse.create().withEntity(response.toString()))
                        }
                    }
                },
                path("remove") {
                    post {
                        parameterList { params ->
                            val walletId = params.find { param -> param.key == "walletId" }?.value as String
                            val funds = params.find { param -> param.key == "funds" }?.value as String

                            val walletRef = sharding.entityRefFor(Wallet.typeKey, walletId)

                            val response = walletRef.ask(
                                { Wallet.Command.ReserveFunds(funds.toInt(), it) },
                                Duration.ofSeconds(3)
                            ).handle { res, _ ->
                                when (res) {
                                    is Wallet.Response.UpdatedResponse.Accepted -> HttpResponse.create()
                                        .withStatus(StatusCodes.ACCEPTED)

                                    is Wallet.Response.UpdatedResponse.Rejected -> HttpResponse.create()
                                        .withStatus(StatusCodes.BAD_REQUEST)
                                        .withEntity("not enough funds in the wallet")
                                }
                            }.toCompletableFuture().get()

                            complete(response)
                        }
                    }
                },
                get {
                    parameterList { params ->
                        val walletId = params.find { param -> param.key == "walletId" }?.value as String
                        val walletRef = sharding.entityRefFor(Wallet.typeKey, walletId)
                        val response = walletRef.ask(
                            { Wallet.Command.CheckFunds(it) },
                            Duration.ofSeconds(3)
                        ).toCompletableFuture().get()

                        complete(response.toString())
                    }
                }
            )
        }
}

