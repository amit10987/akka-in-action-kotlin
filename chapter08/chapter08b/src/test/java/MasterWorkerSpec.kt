import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Routers
import com.typesafe.config.ConfigFactory
import countwords.Master
import countwords.Worker
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

class MasterWorkerSpec {
    companion object {
        val config = ConfigFactory
            .parseString(
                """example.countwords.workers-per-node = 5"""
            )
            .withFallback(ConfigFactory.load("words"))
        val testKit = ActorTestKit.create(config)

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `The words app should send work from the master to the workers and back`() {
        val numberOfWorkers = testKit.system().settings().config().getInt("example.countwords.workers-per-node")

        for (i in 0..numberOfWorkers) {
            testKit.spawn(Worker.create(), "worker-$i")
        }

        val router = testKit.spawn(Routers.group(Worker.RegistrationKey))

        val probe = testKit.createTestProbe(Master.Event::class.java)

        val masterMonitored =
            Behaviors.monitor(Master.Event::class.java, probe.ref, Master.create(router))

        testKit.spawn(masterMonitored, "master0")

        probe.expectMessage(Master.Event.Tick)
        probe.expectMessage(
            Master.Event.CountedWords(
                mapOf(
                    "this" to 1,
                    "a" to 2,
                    "very" to 1,
                    "simulates" to 1,
                    "simple" to 1,
                    "stream" to 2
                )
            )
        )
    }
}