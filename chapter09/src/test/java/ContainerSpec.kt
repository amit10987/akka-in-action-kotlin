import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.javadsl.Entity
import akka.cluster.sharding.typed.javadsl.EntityRef
import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class ContainerSpec {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }


    @Test
    fun `a sharded freight entity should be able to add a cargo`() {
        val sharding = ClusterSharding.get(testKit.system())
        val entityDef = Entity.of(Container.typeKey) { b -> Container.create(b.entityId) }
        val shardRegion: ActorRef<ShardingEnvelope<Container.Command>> = sharding.init(entityDef)
        val containerId = "id-1"
        val cargo = Container.Cargo("id-c", "sack", 3)
        shardRegion.tell(ShardingEnvelope(containerId, Container.Command.AddCargo(cargo)))
        val probe = testKit.createTestProbe<List<Container.Cargo>>()
        val container: EntityRef<Container.Command> = sharding.entityRefFor(Container.typeKey, containerId)
        //shardRegion.tell(ShardingEnvelope(containerId, Container.Command.GetCargos(probe.ref)))
        container.tell(Container.Command.GetCargos(probe.ref))
        probe.expectMessage(listOf(cargo))
    }
}