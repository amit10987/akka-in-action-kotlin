import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.Entity
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import com.typesafe.config.ConfigFactory
import market.domain.Bet
import market.domain.Market
import market.domain.Wallet
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationSpec {

    companion object {
        val config = ConfigFactory
            .parseString(
                """
                      akka.actor.provider = cluster
                      akka.actor.serialization-bindings {
                        "market.domain.CborSerializable" = jackson-cbor
                      } 
                      akka.remote.classic.netty.tcp.port = 0
                      akka.remote.artery.canonical.port = 12345
                      akka.remote.artery.canonical.hostname = 127.0.0.1
                      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
                      akka.persistence.journal.inmem.test-serialization = on
                  """
            )
            .withFallback(ConfigFactory.load("in-memory"))
        val testKit = ActorTestKit.create(config)
        val system = testKit.system()
        val sharding = ClusterSharding.get(system)
    }

    @BeforeAll
    fun beforeAll() {


        Cluster.get(system).manager().tell(Join(Cluster.get(system).selfMember().address()))

        sharding.init(Entity.of(Wallet.typeKey) { Wallet.create(it.entityId) })
        sharding.init(Entity.of(Market.typeKey) { Market.create(it.entityId) })
        sharding.init(Entity.of(Bet.typeKey) { Bet.create(it.entityId) })
    }

    @AfterAll
    fun afterAll() {
        testKit.shutdownTestKit()
    }

    @Test
    fun `a bet should fail if the odds from the market are less than the bet ones`() {
        val walletProbe = testKit.createTestProbe(Wallet.Response.UpdatedResponse::class.java)
        val wallet  = testKit.spawn(Wallet.create("walletId1"))
        //val wallet = sharding.entityRefFor(Wallet.typeKey, "walletId1")

        wallet.tell(Wallet.Command.AddFunds(100, walletProbe.ref))



        println(walletProbe.receiveMessage())
        Thread.sleep(3000)
//        walletProbe.expectMessage(Duration.ofSeconds(10), Wallet.Response.UpdatedResponse.Accepted)
//        val marketProbe = testKit.createTestProbe(Market.Response::class.java)
//        val market = sharding.entityRefFor(Market.typeKey, "marketId1")
//
//        val bet = sharding.entityRefFor(Bet.typeKey, "betId1")
//        val betProbe = testKit.createTestProbe(Bet.Response::class.java)
//
//        bet.tell(
//            Bet.Command.ReplyCommand.Open(
//                "walletId1",
//                "marketId1",
//                1.26,
//                100,
//                0,
//                betProbe.ref
//            )
//        )
//
//        Thread.sleep(3000)
//
//        bet.tell(Bet.Command.ReplyCommand.GetState(betProbe.ref))
//
//        val expected = Bet.State.FailedState(
//            Bet.Status("betId1", "walletId1", "marketId1", 1.26, 100, 0),
//            "market odds [Some(1.05)] not available"
//        )
//
//        betProbe.expectMessage(Bet.Response.CurrentState(expected))
    }


}