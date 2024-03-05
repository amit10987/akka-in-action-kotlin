import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.javadsl.ClusterSharding
import akka.cluster.sharding.typed.javadsl.Entity

fun main() {
    val system = ActorSystem.create(Behaviors.empty<Nothing>(), "containers")
    val sharding = ClusterSharding.get(system)
    val entityDefinition =
        Entity.of(SPContainer.typeKey) { entityContext -> SPContainer.create(entityContext.entityId) }
    val shardRegion: ActorRef<ShardingEnvelope<SPContainer.Command>> = sharding.init(entityDefinition)
    shardRegion.tell(ShardingEnvelope("9", SPContainer.AddCargo(SPContainer.Cargo("456", "sack", 22))))
    shardRegion.tell(ShardingEnvelope("9", SPContainer.AddCargo(SPContainer.Cargo("459", "bigbag", 15))))

    shardRegion.tell(ShardingEnvelope("11", SPContainer.AddCargo(SPContainer.Cargo("499", "barrel", 120))))

}