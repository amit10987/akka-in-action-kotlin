package projection

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import projection.LoggerShardedApp.Companion.logger

class LoggerShardedApp(val tag: String, context: ActorContext<Unit>) : AbstractBehavior<Unit>(context) {
    companion object {
        val logger = LoggerFactory.getLogger("LoggerShardedApp")
        fun create(tag: String): Behavior<Unit> = Behaviors.setup {
            it.log.info("spawned LoggerBehavior {}", tag)
            Behaviors.ignore()
        }
    }

    override fun createReceive(): Receive<Unit> {
        return newReceiveBuilder().build()
    }


}

fun main(args: Array<String>) {
    startUp(args[0].toInt())
}

fun startUp(port: Int) {
    logger.info("starting cluster on port {}", port)
    val config = ConfigFactory
        .parseString(
            """
      akka.remote.artery.canonical.port=$port
      """
        )
        .withFallback(ConfigFactory.load("shardeddeamon"))

    val system = ActorSystem.create<Unit>(Behaviors.empty(), "MiniCluster", config)
    val tags = listOf("container-tag-1", "container-tag-2", "container-tag-3")
    ShardedDaemonProcess.get(system).init(
        Unit::class.java,
        "loggers",
        tags.size
    ) { index -> LoggerShardedApp.create(tags[index])}

}