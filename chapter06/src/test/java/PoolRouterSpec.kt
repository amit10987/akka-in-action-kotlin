import akka.actor.testkit.typed.javadsl.ActorTestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import routers.BroadcastingManager

class PoolRouterSpec {

    companion object {
        val testKit = ActorTestKit.create()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun `a pool router should send messages in round-robing fashion`() {
        val probe = testKit.createTestProbe<String>()
        val worker = routers.Worker.create(probe.ref)
        testKit.spawn(routers.Manager.create(worker), "round-robin")

        probe.expectMessage("hi")
        probe.receiveSeveralMessages(9)
    }

    @Test
    fun `Broadcast, sending each message to all routtes`() {
        val probe = testKit.createTestProbe<String>()
        val worker = routers.Worker.create(probe.ref)

        val router = testKit.spawn(BroadcastingManager.create(worker), "broadcasting")
        probe.expectMessage("hi, there")
        probe.receiveSeveralMessages(43)
    }
}