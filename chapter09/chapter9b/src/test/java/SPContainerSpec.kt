import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.Entity
import akka.cluster.sharding.typed.javadsl.EntityRef
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class SPContainerSpec {
    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `a persistent entity with sharding should be able to add container`() {
        val sharding = ClusterSharding.get(testKit.system())
        val entityDef = Entity.of(SPContainer.typeKey) { b -> SPContainer.create(b.entityId) }
        val shardRegion: ActorRef<ShardingEnvelope<SPContainer.Command>> = sharding.init(entityDef)
        val containerId = "123"
        val cargo = SPContainer.Cargo("id-c", "sack", 3)
        shardRegion.tell(ShardingEnvelope(containerId, SPContainer.AddCargo(cargo)))
        val probe = testKit.createTestProbe<List<SPContainer.Cargo>>()
        val container: EntityRef<SPContainer.Command> = sharding.entityRefFor(SPContainer.typeKey, containerId)
        container.tell(SPContainer.GetCargos(probe.ref))
        probe.expectMessage(listOf(cargo))
    }
}