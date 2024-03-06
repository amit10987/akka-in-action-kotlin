package projection

import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.javadsl.ShardedDaemonProcess
import akka.projection.ProjectionBehavior
import akka.projection.ProjectionBehavior.Command
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import projection.LoggerShardedApp.Companion.logger
import repository.CargosPerContainerRepositoryImpl
import repository.ScalikeJdbcSession
import repository.ScalikeJdbcSetup

class Main {
    val logger = LoggerFactory.getLogger(Main::class.java)
}

fun main(args: Array<String>) {
    logger.info("initializing system")
    val system = if (args.isEmpty()) {
        initActorSystem(0)
    } else {
        initActorSystem(args[0].toInt())
    }

    try {
        ScalikeJdbcSetup.init(system)
        initProjection(system)
    } catch (ex: Exception) {
        logger.error("terminating by NonFatal Exception", ex)
        system.terminate()
    }
}

fun initActorSystem(port: Int): ActorSystem<Nothing> {
    val config = ConfigFactory.parseString(
        """
        akka.remote.artery.canonical.port=$port
        """
    ).withFallback(ConfigFactory.load())

    return ActorSystem.create<Nothing>(
        Behaviors.empty(),
        "containersprojection",
        config
    )
}

fun initProjection(system: ActorSystem<Nothing>) {
    ShardedDaemonProcess.get(system).init(
        Command::class.java,
        "cargos-per-container-projection",
        1
    ) {
        ProjectionBehavior.create(
            CargosPerContainerProjection(ScalikeJdbcSession()).createProjectionFor(
                system,
                CargosPerContainerRepositoryImpl(),
                it
            )
        )
    }
}