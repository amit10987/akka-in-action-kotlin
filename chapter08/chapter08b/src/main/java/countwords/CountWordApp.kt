package countwords

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import akka.actor.typed.javadsl.Routers
import akka.cluster.typed.Cluster
import akka.cluster.typed.SelfUp
import akka.cluster.typed.Subscribe
import com.typesafe.config.ConfigFactory

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        startup("worker", 0)
    } else {
        require(
            args.size == 2
        ) { "Usage: two params required 'role' and 'port'" }
        startup(args[0], args[1].toInt())
    }
}

fun startup(role: String, port: Int) {
    val config = ConfigFactory.parseString(
        """
        akka.remote.artery.canonical.port=$port
        akka.cluster.roles = [$role]
        """
    ).withFallback(ConfigFactory.load("words"))

    val guardian = ActorSystem.create(ClusteredGuardian.create(), "WordsCluster", config)

    println("#################### press ENTER to terminate ###############")
    readLine()
    guardian.terminate()
}

class ClusteredGuardian(context: ActorContext<SelfUp>) : AbstractBehavior<SelfUp>(context) {

    companion object {
        fun create(): Behavior<SelfUp> = Behaviors.setup { context ->
            val cluster = Cluster.get(context.system)
            if (cluster.selfMember().hasRole("director")) {
                Cluster.get(context.system).subscriptions().tell(Subscribe(context.self, SelfUp::class.java))
            }

            if (cluster.selfMember().hasRole("aggregator")) {
                val numberOfWorkers = context.system.settings().config().getInt("example.countwords.workers-per-node")
                for (i in 0 until numberOfWorkers) {
                    context.spawn(Worker.create(), "worker-$i")
                }
            }

            ClusteredGuardian(context)
        }
    }

    override fun createReceive(): Receive<SelfUp> {
        return newReceiveBuilder().onMessage(SelfUp::class.java) {
            val router = context.spawnAnonymous(
                Routers.group(Worker.RegistrationKey)
            )
            context.spawn(Master.create(router), "master")
            Behaviors.same()
        }.build()
    }

}